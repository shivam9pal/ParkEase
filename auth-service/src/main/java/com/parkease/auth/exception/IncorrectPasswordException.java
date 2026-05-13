package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class IncorrectPasswordException extends PasswordException {

    public IncorrectPasswordException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INCORRECT_PASSWORD");
    }
}
