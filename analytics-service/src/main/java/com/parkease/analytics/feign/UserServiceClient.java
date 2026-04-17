package com.parkease.analytics.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.parkease.analytics.feign.dto.UserProfileDto;

@FeignClient(
        name = "auth-service",
        configuration = FeignConfig.class
)
public interface UserServiceClient {

    // Used by: getPlatformSummary() to get total user count
    // Returns direct array of users from auth-service
    @GetMapping("/api/v1/auth/users")
    List<UserProfileDto> getAllUsers(
            @RequestParam(required = false) String role
    );
}
