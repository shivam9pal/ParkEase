package com.parkease.media.exception;

public class FileNotFoundException extends RuntimeException {

    private String errorCode;

    public FileNotFoundException(String message) {
        super(message);
        this.errorCode = "FILE_NOT_FOUND";
    }

    public FileNotFoundException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FILE_NOT_FOUND";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
