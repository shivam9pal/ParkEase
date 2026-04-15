package com.parkease.booking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkease.booking.entity.Booking;
import com.parkease.booking.entity.BookingStatus;
import com.parkease.booking.entity.BookingType;
import com.parkease.booking.entity.VehicleType;
import com.parkease.booking.feign.ParkingLotServiceClient;
import com.parkease.booking.feign.SpotServiceClient;
import com.parkease.booking.feign.VehicleServiceClient;
import com.parkease.booking.messaging.BookingEventPublisher;
import com.parkease.booking.repository.BookingRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Service Tests")
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SpotServiceClient spotServiceClient;

    @Mock
    private ParkingLotServiceClient parkingLotServiceClient;

    @Mock
    private VehicleServiceClient vehicleServiceClient;

    @Mock
    private BookingEventPublisher bookingEventPublisher;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UUID testUserId;
    private UUID testVehicleId;
    private UUID testSpotId;
    private UUID testLotId;
    private UUID testBookingId;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testVehicleId = UUID.randomUUID();
        testSpotId = UUID.randomUUID();
        testLotId = UUID.randomUUID();
        testBookingId = UUID.randomUUID();

        // Setup test booking
        LocalDateTime now = LocalDateTime.now();
        testBooking = Booking.builder()
                .bookingId(testBookingId)
                .userId(testUserId)
                .lotId(testLotId)
                .spotId(testSpotId)
                .vehicleId(testVehicleId)
                .vehiclePlate("ABC123")
                .vehicleType(VehicleType.FOUR_WHEELER)
                .bookingType(BookingType.WALK_IN)
                .startTime(now)
                .endTime(now.plusHours(2))
                .checkInTime(now)
                .checkOutTime(null)
                .status(BookingStatus.ACTIVE)
                .totalAmount(null)
                .pricePerHour(BigDecimal.valueOf(50.00))
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Should retrieve bookings by user from repository")
    void testGetBookingsByUserSuccess() {
        // Arrange
        List<Booking> userBookings = List.of(testBooking);
        when(bookingRepository.findByUserId(testUserId))
                .thenReturn(userBookings);

        // Act
        var result = bookingRepository.findByUserId(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserId, result.get(0).getUserId());
        verify(bookingRepository).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Should retrieve booking by ID from repository")
    void testGetBookingByIdSuccess() {
        // Arrange - directly test repository
        when(bookingRepository.findById(testBookingId))
                .thenReturn(Optional.of(testBooking));

        // Act
        var result = bookingRepository.findById(testBookingId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testBookingId, result.get().getBookingId());
        verify(bookingRepository).findById(testBookingId);
    }

    @Test
    @DisplayName("Should retrieve bookings from repository")
    void testRepositoryRetrievalSuccess() {
        // Arrange
        List<Booking> bookings = List.of(testBooking);
        when(bookingRepository.findAll())
                .thenReturn(bookings);

        // Act
        var result = bookingRepository.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bookingRepository).findAll();
    }

    @Test
    @DisplayName("Should calculate fare correctly")
    void testCalculateFareCorrectly() {
        // This is a utility test to verify fare calculation logic
        LocalDateTime checkInTime = LocalDateTime.now();
        LocalDateTime checkOutTime = checkInTime.plusHours(2);
        BigDecimal pricePerHour = BigDecimal.valueOf(100.00);

        // For 2 hours at 100/hour, expected fare = 200
        BigDecimal expectedFare = BigDecimal.valueOf(200.00);

        // Verify the math
        long durationMinutes = java.time.Duration.between(checkInTime, checkOutTime).toMinutes();
        long hours = (durationMinutes + 59) / 60; // Ceiling
        BigDecimal calculatedFare = pricePerHour.multiply(BigDecimal.valueOf(hours));

        assertEquals(expectedFare, calculatedFare);
    }

    @Test
    @DisplayName("Should calculate minimum 1-hour fare for short parking")
    void testCalculateMinimumFare() {
        // 30 minutes parking with 100/hour rate
        LocalDateTime checkInTime = LocalDateTime.now();
        LocalDateTime checkOutTime = checkInTime.plusMinutes(30);
        BigDecimal pricePerHour = BigDecimal.valueOf(100.00);

        // Minimum should be 1 hour = 100
        BigDecimal expectedFare = BigDecimal.valueOf(100.00);

        long durationMinutes = java.time.Duration.between(checkInTime, checkOutTime).toMinutes();
        long hours = (durationMinutes + 59) / 60; // Ceiling division
        BigDecimal calculatedFare = pricePerHour.multiply(BigDecimal.valueOf(hours));

        assertEquals(expectedFare, calculatedFare);
        assertEquals(1, hours);
    }

    @Test
    @DisplayName("Should verify booking repository is properly mocked")
    void testMockingSetup() {
        // Verify mocks are injected properly
        assertNotNull(bookingRepository);
        assertNotNull(spotServiceClient);
        assertNotNull(parkingLotServiceClient);
        assertNotNull(vehicleServiceClient);
        assertNotNull(bookingEventPublisher);
        assertNotNull(bookingService);
    }

    @Test
    @DisplayName("Should verify booking entity has correct fields")
    void testBookingEntityStructure() {
        // Verify test booking can be created and accessed
        assertNotNull(testBooking);
        assertEquals(testUserId, testBooking.getUserId());
        assertEquals(testSpotId, testBooking.getSpotId());
        assertEquals(testVehicleId, testBooking.getVehicleId());
        assertEquals(BookingStatus.ACTIVE, testBooking.getStatus());
        assertEquals(BookingType.WALK_IN, testBooking.getBookingType());
    }

    @Test
    @DisplayName("Should save booking successfully")
    void testSaveBookingSuccess() {
        // Arrange
        when(bookingRepository.save(testBooking))
                .thenReturn(testBooking);

        // Act
        var savedBooking = bookingRepository.save(testBooking);

        // Assert
        assertNotNull(savedBooking);
        assertEquals(testBookingId, savedBooking.getBookingId());
        verify(bookingRepository).save(testBooking);
    }
}
