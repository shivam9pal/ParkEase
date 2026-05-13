package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class S3UploadException extends ExternalServiceException {

    public S3UploadException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "S3_UPLOAD_ERROR");
    }

    public S3UploadException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_GATEWAY, "S3_UPLOAD_ERROR");
    }
}
