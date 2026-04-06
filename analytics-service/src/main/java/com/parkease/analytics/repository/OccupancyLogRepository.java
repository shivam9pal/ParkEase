package com.parkease.analytics.repository;

import com.parkease.analytics.entity.OccupancyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OccupancyLogRepository extends JpaRepository<OccupancyLog, UUID> {

    // 1. Latest snapshot for a lot (real-time occupancy rate)
    Optional<OccupancyLog> findFirstByLotIdAndEventTypeOrderByTimestampDesc(
            UUID lotId, String eventType);

    // 2. All logs for a lot and event type (spot type utilisation)
    List<OccupancyLog> findByLotIdAndEventType(UUID lotId, String eventType);

    // 3. Logs for hourly breakdown (last N days, SCHEDULED only)
    List<OccupancyLog> findByLotIdAndEventTypeAndTimestampAfter(
            UUID lotId, String eventType, LocalDateTime after);

    // 4. Logs in a time range (daily report)
    List<OccupancyLog> findByLotIdAndTimestampBetween(
            UUID lotId, LocalDateTime start, LocalDateTime end);

    // 5. Count bookings/checkouts in a time range
    long countByLotIdAndEventTypeAndTimestampBetween(
            UUID lotId, String eventType, LocalDateTime start, LocalDateTime end);

    // 6. Logs by lot + eventType + time range (revenue + occupancy reports)
    List<OccupancyLog> findByLotIdAndEventTypeAndTimestampBetween(
            UUID lotId, String eventType, LocalDateTime start, LocalDateTime end);

    // 7. Average parking duration for a specific lot (CHECKOUT events only)
    @Query("SELECT AVG(o.durationMinutes) FROM OccupancyLog o " +
            "WHERE o.lotId = :lotId AND o.eventType = 'CHECKOUT' " +
            "AND o.durationMinutes IS NOT NULL")
    Double avgDurationByLotId(@Param("lotId") UUID lotId);

    // 8. Platform-wide average parking duration
    @Query("SELECT AVG(o.durationMinutes) FROM OccupancyLog o " +
            "WHERE o.eventType = 'CHECKOUT' AND o.durationMinutes IS NOT NULL")
    Double avgDurationPlatform();

    // 9. Data retention cleanup (optional scheduled cleanup)
    @Modifying
    @Query("DELETE FROM OccupancyLog o WHERE o.timestamp < :cutoff")
    int deleteLogsOlderThan(@Param("cutoff") LocalDateTime cutoff);
}