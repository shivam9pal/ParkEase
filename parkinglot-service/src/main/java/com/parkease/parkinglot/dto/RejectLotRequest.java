package com.parkease.parkinglot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RejectLotRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
