package com.parkease.analytics.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.parkease.analytics.feign.dto.UserCountDto;

@FeignClient(
        name = "auth-service",
        configuration = FeignConfig.class
)
public interface UserServiceClient {

    // Used by: getPlatformSummary() to get total user count
    @GetMapping("/api/v1/auth/users")
    UserCountDto getAllUsers(
            @RequestParam(required = false) String role
    );
}
