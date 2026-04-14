package com.parkease.payment.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.razorpay.RazorpayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkease.payment.dto.DailyRevenueResponse;
import com.parkease.payment.dto.InitiatePaymentRequest;
import com.parkease.payment.dto.PaymentResponse;
import com.parkease.payment.dto.PaymentStatusResponse;
import com.parkease.payment.dto.PaymentSummaryResponse;
import com.parkease.payment.dto.RevenueResponse;
import com.parkease.payment.entity.Payment;
import com.parkease.payment.enums.PaymentMode;
import com.parkease.payment.enums.PaymentStatus;
import com.parkease.payment.exception.ConflictException;
import com.parkease.payment.exception.ResourceNotFoundException;
import com.parkease.payment.exception.ServiceUnavailableException;
import com.parkease.payment.feign.BookingServiceClient;
import com.parkease.payment.feign.dto.BookingDetailDto;
import com.parkease.payment.rabbitmq.PaymentEventPublisher;
import com.parkease.payment.rabbitmq.dto.BookingEventPayload;
import com.parkease.payment.repository.PaymentRepository;

import org.json.JSONObject;

import com.razorpay.RazorpayException;
import com.parkease.payment.dto.CreateRazorpayOrderRequest;
import com.parkease.payment.dto.RazorpayOrderResponse;
import com.parkease.payment.dto.VerifyRazorpayPaymentRequest;


import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final BookingServiceClient bookingServiceClient;
    private final ReceiptGeneratorService receiptGeneratorService;

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    // ─── RabbitMQ: booking.checkout ───────────────────────────────────────────
    @Override
    @Transactional
    public PaymentResponse processPaymentFromEvent(BookingEventPayload payload) {
        Optional<Payment> existing = paymentRepository.findByBookingId(payload.getBookingId());
        if (existing.isPresent()) {
            log.warn("Idempotency hit — payment already exists for bookingId={}", payload.getBookingId());
            return toResponse(existing.get());
        }

        Payment payment = Payment.builder()
                .bookingId(payload.getBookingId())
                .userId(payload.getUserId())
                .lotId(payload.getLotId())
                .amount(payload.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .mode(null)
                .transactionId(null)
                .currency("INR")
                .paidAt(null)
                .description("Parking fee for booking " + payload.getBookingId())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("[Checkout Event] Payment PENDING created for bookingId={}, paymentId={}, amount={}", payload.getBookingId(), saved.getPaymentId(), saved.getAmount());
        return toResponse(saved);
    }

    // ─── RabbitMQ: booking.cancelled ──────────────────────────────────────────
    @Override
    @Transactional
    public void processRefundFromEvent(BookingEventPayload payload) {
        Optional<Payment> optional = paymentRepository.findByBookingId(payload.getBookingId());
        if (optional.isEmpty()) {
            log.warn("No payment found for bookingId={} — cancelled before payment, skipping", payload.getBookingId());
            return;
        }

        Payment payment = optional.get();

        switch (payment.getStatus()) {
            case PAID -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("Refund processed for bookingId={}", payload.getBookingId());
                paymentEventPublisher.publishPaymentRefunded(toResponse(payment));
            }
            case PENDING -> {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.info("Payment voided (FAILED) for cancelled booking bookingId={}", payload.getBookingId());
            }
            default ->
                log.warn("Payment in terminal status={} for bookingId={}, skipping",
                        payment.getStatus(), payload.getBookingId());
        }
    }

    // ─── REST: Manual Initiate ─────────────────────────────────────────────────
    @Override
    @Transactional
    public PaymentResponse initiatePayment(UUID userId, InitiatePaymentRequest request) {

        // ✅ NEW GUARD: Non-cash must go through Razorpay
        if (request.getMode() != PaymentMode.CASH) {
            throw new IllegalArgumentException(
                    "For CARD/UPI/WALLET, use POST /api/v1/payments/razorpay/create-order instead.");
        }
        BookingDetailDto booking = fetchBooking(request.getBookingId());

        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Only COMPLETED bookings can be paid. Current status: " + booking.getStatus());
        }
        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("You do not own this booking");
        }

        String transactionId = (request.getMode() == PaymentMode.CASH) ? null : UUID.randomUUID().toString();

        // Try to find existing PENDING payment (from checkout event)
        Optional<Payment> existingPayment = paymentRepository.findByBookingId(request.getBookingId());

        Payment payment;
        if (existingPayment.isPresent()) {
            // Update existing PENDING payment to PAID
            payment = existingPayment.get();

            if (payment.getStatus() == PaymentStatus.PAID) {
                throw new ConflictException("Payment already completed for this booking");
            }

            payment.setStatus(PaymentStatus.PAID);
            payment.setMode(request.getMode());
            payment.setTransactionId(transactionId);
            payment.setPaidAt(LocalDateTime.now());
            log.info("[Initiate Payment] Updated PENDING payment to PAID - bookingId={}, paymentId={}, amount={}",
                    request.getBookingId(), payment.getPaymentId(), payment.getAmount());
        } else {
            // Create new PAID payment (for manual payment without prior checkout)
            payment = Payment.builder()
                    .bookingId(request.getBookingId())
                    .userId(userId)
                    .lotId(booking.getLotId())
                    .amount(booking.getTotalAmount())
                    .status(PaymentStatus.PAID)
                    .mode(request.getMode())
                    .transactionId(transactionId)
                    .currency("INR")
                    .paidAt(LocalDateTime.now())
                    .description("Parking fee for booking " + request.getBookingId())
                    .build();
            log.info("[Initiate Payment] Created new PAID payment (no checkout event) - bookingId={}, amount={}",
                    request.getBookingId(), payment.getAmount());
        }

        Payment saved = paymentRepository.save(payment);
        paymentEventPublisher.publishPaymentCompleted(toResponse(saved));
        log.info("[Initiate Payment] Payment completed - bookingId={}, paymentId={}, mode={}",
                request.getBookingId(), saved.getPaymentId(), request.getMode());
        return toResponse(saved);
    }

    // ─── Read Operations ───────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(UUID bookingId, UUID requesterId, String requesterRole) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for bookingId: " + bookingId));
        enforceOwnership(payment, requesterId, requesterRole);
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId, UUID requesterId, String requesterRole) {
        Payment payment = findPaymentById(paymentId);
        enforceOwnership(payment, requesterId, requesterRole);
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUser(UUID userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByLot(UUID lotId) {
        return paymentRepository.findByLotId(lotId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Payment not found for transactionId: " + transactionId));
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID paymentId, UUID requesterId, String requesterRole) {
        Payment payment = findPaymentById(paymentId);
        enforceOwnership(payment, requesterId, requesterRole);
        return PaymentStatusResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .status(payment.getStatus().name())
                .paidAt(payment.getPaidAt())
                .refundedAt(payment.getRefundedAt())
                .build();
    }

    // ─── Refund ────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PaymentResponse processManualRefund(UUID paymentId) {
        Payment payment = findPaymentById(paymentId);
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Only PAID payments can be refunded. Current status: " + payment.getStatus());
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        paymentEventPublisher.publishPaymentRefunded(toResponse(payment));
        log.info("Manual refund processed for paymentId={}", paymentId);
        return toResponse(payment);
    }

    // ─── Revenue ───────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public RevenueResponse getLotRevenue(UUID lotId, LocalDateTime from, LocalDateTime to) {
        List<Payment> payments = paymentRepository
                .findByLotIdAndStatusAndPaidAtBetween(lotId, PaymentStatus.PAID, from, to);
        BigDecimal total = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return RevenueResponse.builder()
                .lotId(lotId).from(from).to(to)
                .totalRevenue(total).currency("INR")
                .transactionCount(payments.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyRevenueResponse> getLotDailyRevenue(UUID lotId, LocalDateTime from, LocalDateTime to) {
        List<Payment> payments = paymentRepository
                .findByLotIdAndStatusAndPaidAtBetween(lotId, PaymentStatus.PAID, from, to);

        Map<LocalDate, List<Payment>> grouped = payments.stream()
                .collect(Collectors.groupingBy(p -> p.getPaidAt().toLocalDate()));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> DailyRevenueResponse.builder()
                .date(e.getKey())
                .revenue(e.getValue().stream()
                        .map(Payment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .transactionCount(e.getValue().size())
                .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueResponse getPlatformRevenue(LocalDateTime from, LocalDateTime to) {
        List<Payment> payments = paymentRepository
                .findByStatusAndPaidAtBetween(PaymentStatus.PAID, from, to);
        BigDecimal total = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return RevenueResponse.builder()
                .lotId(null).from(from).to(to)
                .totalRevenue(total).currency("INR")
                .transactionCount(payments.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentSummaryResponse> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    // ─── Receipt ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public byte[] generateAndGetReceipt(UUID paymentId, UUID requesterId, String requesterRole) {
        log.info("[Receipt] Generating receipt for paymentId={}, requesterId={}, role={}", paymentId, requesterId, requesterRole);

        Payment payment = findPaymentById(paymentId);
        log.info("[Receipt] Payment found - Status: {}, UserId: {}", payment.getStatus(), payment.getUserId());

        enforceOwnership(payment, requesterId, requesterRole);
        log.info("[Receipt] Ownership verified");

        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.REFUNDED) {
            log.error("[Receipt] Invalid status for receipt: {}", payment.getStatus());
            throw new IllegalStateException(
                    "Receipt can only be generated for PAID or REFUNDED payments. Current status: " + payment.getStatus());
        }
        log.info("[Receipt] Status check passed: {}", payment.getStatus());

        if (payment.getReceiptPath() != null && Files.exists(Paths.get(payment.getReceiptPath()))) {
            log.info("[Receipt] Using cached receipt from: {}", payment.getReceiptPath());
            try {
                return Files.readAllBytes(Paths.get(payment.getReceiptPath()));
            } catch (IOException e) {
                log.warn("[Receipt] Cached receipt unreadable, regenerating: {}", e.getMessage());
            }
        }

        log.info("[Receipt] Fetching booking details for bookingId={}", payment.getBookingId());
        BookingDetailDto booking = fetchBooking(payment.getBookingId());
        log.info("[Receipt] Booking fetched successfully");

        try {
            log.info("[Receipt] Generating new receipt PDF");
            String path = receiptGeneratorService.generateReceipt(payment, booking);
            log.info("[Receipt] PDF generated at: {}", path);

            payment.setReceiptPath(path);
            paymentRepository.save(payment);
            log.info("[Receipt] Receipt path saved to database");

            byte[] bytes = Files.readAllBytes(Paths.get(path));
            log.info("[Receipt] Receipt bytes read successfully, size: {} bytes", bytes.length);
            return bytes;
        } catch (IOException e) {
            log.error("[Receipt] IOException during receipt generation for paymentId={}: {}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Receipt generation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("[Receipt] Unexpected error during receipt generation for paymentId={}: {}", paymentId, e.getMessage(), e);
            throw e;
        }
    }

    // ─── Private Helpers ───────────────────────────────────────────────────────
    private Payment findPaymentById(UUID paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
    }

    private void enforceOwnership(Payment payment, UUID requesterId, String requesterRole) {
        if ("DRIVER".equals(requesterRole) && !payment.getUserId().equals(requesterId)) {
            throw new SecurityException("Access denied: You do not own this payment");
        }
    }

    private BookingDetailDto fetchBooking(UUID bookingId) {
        try {
            return bookingServiceClient.getBookingById(bookingId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Booking not found with id: " + bookingId);
        } catch (FeignException e) {
            throw new ServiceUnavailableException("Booking service unavailable");
        }
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .bookingId(p.getBookingId())
                .userId(p.getUserId())
                .lotId(p.getLotId())
                .amount(p.getAmount())
                .status(p.getStatus().name())
                .mode(p.getMode() != null ? p.getMode().name() : null)  // ✅ FIX: was p.getMode().name() — NPE for PENDING
                .transactionId(p.getTransactionId())
                .currency(p.getCurrency())
                .paidAt(p.getPaidAt())
                .refundedAt(p.getRefundedAt())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PaymentSummaryResponse toSummaryResponse(Payment p) {
        LocalDateTime updatedAt = p.getRefundedAt() != null ? p.getRefundedAt() : p.getPaidAt();
        return PaymentSummaryResponse.builder()
                .paymentId(p.getPaymentId())
                .amount(p.getAmount())
                .status(p.getStatus())
                .mode(p.getMode())
                .transactionId(p.getTransactionId())
                .createdAt(p.getCreatedAt())
                .updatedAt(updatedAt)
                .build();
    }


    // ─── Razorpay: Step 1 — Create Order ──────────────────────────────────────
    @Override
    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(UUID userId, CreateRazorpayOrderRequest request) {
        BookingDetailDto booking = fetchBooking(request.getBookingId());

        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Only COMPLETED bookings can be paid. Current status: " + booking.getStatus());
        }
        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("You do not own this booking.");
        }

        // Find existing PENDING payment or create one
        Payment payment = paymentRepository.findByBookingId(request.getBookingId())
                .orElseGet(() -> {
                    Payment newPayment = Payment.builder()
                            .bookingId(request.getBookingId())
                            .userId(userId)
                            .lotId(booking.getLotId())
                            .amount(booking.getTotalAmount())
                            .status(PaymentStatus.PENDING)
                            .currency("INR")
                            .description("Parking fee for booking " + request.getBookingId())
                            .build();
                    return paymentRepository.save(newPayment);
                });

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Payment already completed for this booking.");
        }

        // Convert INR → paise (Razorpay requires smallest currency unit)
        long amountInPaise = booking.getTotalAmount()
                .multiply(BigDecimal.valueOf(100)).longValue();

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", payment.getPaymentId().toString());
            orderRequest.put("payment_capture", 1); // auto-capture

            com.razorpay.Order order = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = order.get("id");

            payment.setRazorpayOrderId(razorpayOrderId);
            paymentRepository.save(payment);

            log.info("[Razorpay] Order created — paymentId={}, razorpayOrderId={}, amount={}",
                    payment.getPaymentId(), razorpayOrderId, booking.getTotalAmount());

            return RazorpayOrderResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .razorpayKeyId(razorpayKeyId)    // frontend needs this to open Checkout
                    .amount(booking.getTotalAmount())
                    .amountInPaise(amountInPaise)
                    .currency("INR")
                    .paymentId(payment.getPaymentId())
                    .bookingId(request.getBookingId())
                    .build();

        } catch (RazorpayException e) {
            log.error("[Razorpay] Order creation failed for bookingId={}: {}", request.getBookingId(), e.getMessage());
            throw new ServiceUnavailableException("Payment gateway unavailable. Please try again.");
        }
    }

    // ─── Razorpay: Step 2 — Verify Signature & Capture ────────────────────────

    @Transactional
    @Override
    public PaymentResponse verifyAndCaptureRazorpayPayment(VerifyRazorpayPaymentRequest request) {
        Payment payment = findPaymentById(request.getPaymentId());

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Payment already completed.");
        }

        // Verify HMAC-SHA256: signature = HMAC(razorpayOrderId + "|" + razorpayPaymentId, keySecret)
        String expectedSignature = generateHmacSha256(
                request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
                razorpayKeySecret
        );

        if (!expectedSignature.equals(request.getRazorpaySignature())) {
            log.error("[Razorpay] Signature mismatch for paymentId={} — possible tampering", request.getPaymentId());
            throw new SecurityException("Invalid payment signature. Payment rejected.");
        }

        // Signature valid — mark PAID
        payment.setStatus(PaymentStatus.PAID);
        payment.setMode(request.getMode());
        payment.setTransactionId(request.getRazorpayPaymentId());   // real gateway tx ID
        payment.setRazorpayOrderId(request.getRazorpayOrderId());
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        paymentEventPublisher.publishPaymentCompleted(toResponse(saved));

        log.info("[Razorpay] Payment verified & captured — paymentId={}, razorpayPaymentId={}",
                saved.getPaymentId(), request.getRazorpayPaymentId());

        return toResponse(saved);
    }

    // ─── HMAC-SHA256 Helper ────────────────────────────────────────────────────
    private String generateHmacSha256(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC generation failed", e);
        }
    }
}
