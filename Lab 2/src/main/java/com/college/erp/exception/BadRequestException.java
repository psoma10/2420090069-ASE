package com.college.erp.exception;

/** Raised when input is valid JSON but breaks a business rule. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
