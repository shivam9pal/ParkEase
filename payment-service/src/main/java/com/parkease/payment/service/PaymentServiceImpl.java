package com.parkease.payment.service;

import com.parkease.payment.dto.*;
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
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository       paymentRepository;
    private final PaymentEventPublisher   paymentEventPublisher;
    private final BookingServiceClient    bookingServiceClient;
    private final ReceiptGeneratorService receiptGeneratorService;

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
                .status(PaymentStatus.PAID)
                .mode(PaymentMode.CASH)
                .transactionId(UUID.randomUUID().toString())
                .currency("INR")
                .paidAt(LocalDateTime.now())
                .description("Parking fee for booking " + payload.getBookingId())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment auto-processed for bookingId={}, paymentId={}", payload.getBookingId(), saved.getPaymentId());
        paymentEventPublisher.publishPaymentCompleted(toResponse(saved));
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
            default -> log.warn("Payment in terminal status={} for bookingId={}, skipping",
                    payment.getStatus(), payload.getBookingId());
        }
    }

    // ─── REST: Manual Initiate ─────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse initiatePayment(UUID userId, InitiatePaymentRequest request) {
        BookingDetailDto booking = fetchBooking(request.getBookingId());

        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "Only COMPLETED bookings can be paid. Current status: " + booking.getStatus());
        }
        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("You do not own this booking");
        }

        paymentRepository.findByBookingId(request.getBookingId()).ifPresent(p -> {
            throw new ConflictException("Payment already processed for this booking");
        });

        String transactionId = (request.getMode() == PaymentMode.CASH) ? null : UUID.randomUUID().toString();

        Payment payment = Payment.builder()
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

        Payment saved = paymentRepository.save(payment);
        paymentEventPublisher.publishPaymentCompleted(toResponse(saved));
        log.info("Manual payment initiated for bookingId={}, paymentId={}", request.getBookingId(), saved.getPaymentId());
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

    // ─── Receipt ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public byte[] generateAndGetReceipt(UUID paymentId, UUID requesterId, String requesterRole) {
        Payment payment = findPaymentById(paymentId);
        enforceOwnership(payment, requesterId, requesterRole);

        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.REFUNDED) {
            throw new IllegalStateException(
                    "Receipt can only be generated for PAID or REFUNDED payments");
        }

        if (payment.getReceiptPath() != null && Files.exists(Paths.get(payment.getReceiptPath()))) {
            try {
                return Files.readAllBytes(Paths.get(payment.getReceiptPath()));
            } catch (IOException e) {
                log.warn("Cached receipt unreadable, regenerating: {}", e.getMessage());
            }
        }

        BookingDetailDto booking = fetchBooking(payment.getBookingId());

        try {
            String path = receiptGeneratorService.generateReceipt(payment, booking);
            payment.setReceiptPath(path);
            paymentRepository.save(payment);
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            log.error("Receipt generation failed for paymentId={}: {}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Receipt generation failed");
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
                .mode(p.getMode().name())
                .transactionId(p.getTransactionId())
                .currency(p.getCurrency())
                .paidAt(p.getPaidAt())
                .refundedAt(p.getRefundedAt())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .build();
    }
}