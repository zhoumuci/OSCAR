package com.oscar.backend.service;

import org.springframework.http.HttpStatus;

public class BedtoolsQueryException extends RuntimeException {

    private final String status;
    private final HttpStatus httpStatus;

    public BedtoolsQueryException(String status, String message, HttpStatus httpStatus) {
        super(message);
        this.status = status;
        this.httpStatus = httpStatus;
    }

    public String getStatus() {
        return status;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
