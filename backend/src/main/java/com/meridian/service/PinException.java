package com.meridian.service;

import org.springframework.http.HttpStatus;

/** Thrown for wrong/locked PIN. Transactions are configured NOT to roll back on this one so attempt counters persist. */
public class PinException extends BankException {
    public PinException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "PIN_INVALID");
    }
}
