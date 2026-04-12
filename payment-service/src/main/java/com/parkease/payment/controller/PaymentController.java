package com.parkease.payment.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parkease.payment.dto.DailyRevenueResponse;
import com.parkease.payment.dto.InitiatePaymentRequest;
import com.parkease.payment.dto.PaymentResponse;
import com.parkease.payment.dto.PaymentStatusResponse;
import com.parkease.payment.dto.PaymentSummaryResponse;
import com.parkease.payment.dto.RevenueResponse;
import com.parkease.payment.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // ─── 6.1 Initiate Payment (DRIVER) ────────────────────────────────────────
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        PaymentResponse response = paymentService.initiatePayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── 6.2 Get Payment by Booking ID ────────────────────────────────────────
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID requesterId = extractUserId(authentication);
        String requesterRole = extractRole(authentication);
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId, requesterId, requesterRole));
    }

    // ─── 6.3 Get All Payments by User ─────────────────────────────────────────
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUser(
            @PathVariable UUID userId,
            Authentication authentication) {

        UUID requesterId = extractUserId(authentication);
        String requesterRole = extractRole(authentication);

        // DRIVER can only fetch their own; ADMIN unrestricted
        if ("DRIVER".equals(requesterRole) && !requesterId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }

    // ─── 6.4 Get Payment by Transaction ID (ADMIN) ────────────────────────────
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getByTransactionId(
            @PathVariable String transactionId) {

        return ResponseEntity.ok(paymentService.getPaymentByTransactionId(transactionId));
    }

    // ─── 6.4b Get Payments by Lot (ADMIN) ──────────────────────────────────────
    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByLot(
            @PathVariable UUID lotId) {

        return ResponseEntity.ok(paymentService.getPaymentsByLot(lotId));
    }

    // ─── 6.5 Get Payment Status ───────────────────────────────────────────────
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable UUID paymentId,
            Authentication authentication) {

        UUID requesterId = extractUserId(authentication);
        String requesterRole = extractRole(authentication);
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId, requesterId, requesterRole));
    }

    // ─── 6.6 Manual Refund (MANAGER / ADMIN) ──────────────────────────────────
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> processRefund(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.processManualRefund(paymentId));
    }

    // ─── 6.7 Revenue for a Lot ────────────────────────────────────────────────
    @GetMapping("/revenue/lot/{lotId}")
    public ResponseEntity<RevenueResponse> getLotRevenue(
            @PathVariable UUID lotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(paymentService.getLotRevenue(lotId, from, to));
    }

    // ─── 6.8 Daily Revenue for a Lot ─────────────────────────────────────────
    @GetMapping("/revenue/lot/{lotId}/daily")
    public ResponseEntity<List<DailyRevenueResponse>> getLotDailyRevenue(
            @PathVariable UUID lotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(paymentService.getLotDailyRevenue(lotId, from, to));
    }

    // ─── 6.9 Platform Revenue (ADMIN) ─────────────────────────────────────────
    @GetMapping("/revenue/platform")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RevenueResponse> getPlatformRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(paymentService.getPlatformRevenue(from, to));
    }

    // ─── 6.10 Payment History (DRIVER) ────────────────────────────────────────
    @GetMapping("/history")
    public ResponseEntity<List<PaymentResponse>> getHistory(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }

    // ─── 6.11 Download PDF Receipt ────────────────────────────────────────────
    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<Resource> downloadReceipt(
            @PathVariable UUID paymentId,
            Authentication authentication) {

        UUID requesterId = extractUserId(authentication);
        String requesterRole = extractRole(authentication);

        byte[] pdfBytes = paymentService.generateAndGetReceipt(paymentId, requesterId, requesterRole);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"receipt_" + paymentId + ".pdf\"")
                .body(new ByteArrayResource(pdfBytes));
    }

    // ─── 6.11 Get All Payments (ADMIN only) ────────────────────────────────────
    @GetMapping("/all")
    public ResponseEntity<List<PaymentSummaryResponse>> getAllPayments(
            Authentication authentication) {

        String requesterRole = extractRole(authentication);

        if (!"ADMIN".equals(requesterRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    // ─── JWT Extraction Helpers ───────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getDetails() == null) {
            throw new IllegalStateException("Missing authentication or JWT claims");
        }

        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        Object userIdObj = details.get("userId");

        if (userIdObj == null) {
            throw new IllegalStateException("Missing userId claim in JWT token");
        }

        return UUID.fromString((String) userIdObj);
    }

    private String extractRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "DRIVER";  // Default to DRIVER if no authentication
        }

        return authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("DRIVER");
    }
}
