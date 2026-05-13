package com.parkease.payment.entity;

import com.parkease.payment.enums.PaymentMode;
import com.parkease.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = true, length = 20)  // ✅ FIX: was nullable = false, breaks PENDING payments
    private PaymentMode mode;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    // ✅ NEW: Razorpay Order ID (e.g. order_XXXXXXXXXXXXXXX)
    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    // ✅ NEW: Razorpay Payment ID (e.g. pay_XXXXXXXXXXXXXXX) — set after user pays
    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "receipt_path", length = 500)
    private String receiptPath;

    @Column(name = "receipt_upload_id")
    private UUID receiptUploadId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.currency == null) {
            this.currency = "INR";
        }
    }
}
