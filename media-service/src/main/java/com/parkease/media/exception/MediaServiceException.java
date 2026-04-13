package com.parkease.media.exception;

public class MediaServiceException extends RuntimeException {

    public MediaServiceException(String message) {
        super(message);
    }

    public MediaServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
