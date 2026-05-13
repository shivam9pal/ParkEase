package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountDeactivatedException extends AuthenticationException {

    public AccountDeactivatedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "ACCOUNT_INACTIVE");
    }
}
