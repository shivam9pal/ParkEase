package com.parkease.payment.service;

import com.parkease.payment.dto.*;
import com.parkease.payment.rabbitmq.dto.BookingEventPayload;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse processPaymentFromEvent(BookingEventPayload payload);

    void processRefundFromEvent(BookingEventPayload payload);

    PaymentResponse initiatePayment(UUID userId, InitiatePaymentRequest request);

    PaymentResponse getPaymentByBookingId(UUID bookingId, UUID requesterId, String requesterRole);

    PaymentResponse getPaymentById(UUID paymentId, UUID requesterId, String requesterRole);

    List<PaymentResponse> getPaymentsByUser(UUID userId);

    PaymentResponse getPaymentByTransactionId(String transactionId);

    PaymentStatusResponse getPaymentStatus(UUID paymentId, UUID requesterId, String requesterRole);

    PaymentResponse processManualRefund(UUID paymentId);

    RevenueResponse getLotRevenue(UUID lotId, LocalDateTime from, LocalDateTime to);

    List<DailyRevenueResponse> getLotDailyRevenue(UUID lotId, LocalDateTime from, LocalDateTime to);

    RevenueResponse getPlatformRevenue(LocalDateTime from, LocalDateTime to);

    byte[] generateAndGetReceipt(UUID paymentId, UUID requesterId, String requesterRole);
}