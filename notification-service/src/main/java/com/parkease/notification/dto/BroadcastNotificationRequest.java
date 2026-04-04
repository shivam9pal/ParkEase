package com.parkease.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastNotificationRequest {

    @NotBlank(message = "targetRole is required. Values: DRIVER, MANAGER, ALL")
    private String targetRole;

    @NotBlank(message = "title is required.")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "message is required.")
    @Size(max = 1000)
    private String message;
}