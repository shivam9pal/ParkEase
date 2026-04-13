package com.parkease.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper for Media Service API responses Matches: ApiResponse<T> from
 * media-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseWrapper<T> {

    private boolean success;

    private String message;

    private T data;

    private String errorCode;
}
