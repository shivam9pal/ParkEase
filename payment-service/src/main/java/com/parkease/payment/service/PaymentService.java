package com.parkease.payment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.parkease.payment.dto.*;
import com.parkease.payment.rabbitmq.dto.BookingEventPayload;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentService {

    PaymentResponse processPaymentFromEvent(BookingEventPayload payload);

    void processRefundFromEvent(BookingEventPayload payload);

    PaymentResponse initiatePayment(UUID userId, InitiatePaymentRequest request);

    PaymentResponse getPaymentByBookingId(UUID bookingId, UUID requesterId, String requesterRole);

    PaymentResponse getPaymentById(UUID paymentId, UUID requesterId, String requesterRole);

    List<PaymentResponse> getPaymentsByUser(UUID userId);

    List<PaymentResponse> getPaymentsByLot(UUID lotId);

    PaymentResponse getPaymentByTransactionId(String transactionId);

    PaymentStatusResponse getPaymentStatus(UUID paymentId, UUID requesterId, String requesterRole);

    PaymentResponse processManualRefund(UUID paymentId);

    RevenueResponse getLotRevenue(UUID lotId, LocalDateTime from, LocalDateTime to);

    List<DailyRevenueResponse> getLotDailyRevenue(UUID lotId, LocalDateTime from, LocalDateTime to);

    RevenueResponse getPlatformRevenue(LocalDateTime from, LocalDateTime to);

    List<PaymentSummaryResponse> getAllPayments();

    ReceiptResponse generateAndGetReceipt(UUID paymentId, UUID requesterId, String requesterRole);

    RazorpayOrderResponse createRazorpayOrder(UUID userId, CreateRazorpayOrderRequest request);

    PaymentResponse verifyAndCaptureRazorpayPayment(VerifyRazorpayPaymentRequest request);
}
