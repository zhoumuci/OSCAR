package com.oscar.backend.controller;

import com.oscar.backend.entity.BedtoolsErrorResponse;
import com.oscar.backend.service.BedtoolsQueryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BedtoolsQueryExceptionHandler {

    @ExceptionHandler(BedtoolsQueryException.class)
    public ResponseEntity<BedtoolsErrorResponse> handleBedtoolsQueryException(BedtoolsQueryException exception) {
        return ResponseEntity
                .status(exception.getHttpStatus())
                .body(new BedtoolsErrorResponse(exception.getStatus(), exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BedtoolsErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception
    ) {
        String detail = summarize(exception);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new BedtoolsErrorResponse(
                        "INVALID_REQUEST_BODY",
                        "Request body must be valid JSON" + (detail.isEmpty() ? "" : ": " + detail)
                ));
    }

    private String summarize(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause != null ? cause.getMessage() : exception.getMessage();
        if (message == null || message.isBlank()) {
            return "";
        }
        String normalized = message.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300);
    }
}
