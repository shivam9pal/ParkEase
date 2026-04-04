package com.parkease.payment.repository;

import com.parkease.payment.entity.Payment;
import com.parkease.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentId(UUID paymentId);

    Optional<Payment> findByBookingId(UUID bookingId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Payment> findByLotIdAndStatusAndPaidAtBetween(
            UUID lotId, PaymentStatus status,
            LocalDateTime from, LocalDateTime to);

    List<Payment> findByLotIdAndStatus(UUID lotId, PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByStatus(PaymentStatus status);

    long countByUserId(UUID userId);

    List<Payment> findByStatusAndPaidAtBetween(
            PaymentStatus status,
            LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.lotId = :lotId AND p.status = 'PAID' " +
            "AND p.paidAt BETWEEN :from AND :to")
    BigDecimal sumRevenueByLotIdAndDateRange(
            @Param("lotId") UUID lotId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.status = 'PAID' AND p.paidAt BETWEEN :from AND :to")
    BigDecimal sumPlatformRevenue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}