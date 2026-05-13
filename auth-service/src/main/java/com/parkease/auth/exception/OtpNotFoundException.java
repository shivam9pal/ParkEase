package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class OtpNotFoundException extends OtpException {

    public OtpNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "OTP_NOT_FOUND");
    }
}
