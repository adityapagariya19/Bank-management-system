package com.meridian.service;

import org.springframework.http.HttpStatus;

public class BankException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public BankException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public BankException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public BankException(String message) {
        this(message, HttpStatus.BAD_REQUEST, null);
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
