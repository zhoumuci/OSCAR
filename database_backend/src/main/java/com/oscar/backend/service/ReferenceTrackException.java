package com.oscar.backend.service;

import org.springframework.http.HttpStatus;

public class ReferenceTrackException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String status;

    public ReferenceTrackException(HttpStatus httpStatus, String status, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.status = status;
    }

    public ReferenceTrackException(HttpStatus httpStatus, String status, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.status = status;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getStatus() {
        return status;
    }
}
