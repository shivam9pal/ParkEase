package com.parkease.auth.exception;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiError {
    private LocalDateTime timestamp;
    private int           status;
    private String        error;
    private String        message;
    private String        path;
    private List<String>  errors;   // field-level validation errors
}