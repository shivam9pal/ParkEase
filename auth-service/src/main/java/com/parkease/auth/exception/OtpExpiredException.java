package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class OtpExpiredException extends OtpException {

    public OtpExpiredException(String message) {
        super(message, HttpStatus.GONE, "OTP_EXPIRED");
    }
}
