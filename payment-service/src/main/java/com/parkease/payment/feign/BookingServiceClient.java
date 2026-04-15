package com.parkease.payment.feign;

import com.parkease.payment.feign.dto.BookingDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "booking-service",
//        url = "${services.booking.url}",
        configuration = FeignConfig.class
)
public interface BookingServiceClient {

    @GetMapping("/api/v1/bookings/{bookingId}")
    BookingDetailDto getBookingById(@PathVariable("bookingId") UUID bookingId);
}