package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends AuthenticationException {

    public TokenExpiredException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED");
    }
}
