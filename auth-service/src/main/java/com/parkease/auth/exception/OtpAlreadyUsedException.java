package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class OtpAlreadyUsedException extends OtpException {

    public OtpAlreadyUsedException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "OTP_USED");
    }
}
