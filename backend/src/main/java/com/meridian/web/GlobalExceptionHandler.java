package com.meridian.web;

import com.meridian.service.BankException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, String>> body(HttpStatus s, String message, String code) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("message", message);
        if (code != null) m.put("code", code);
        return ResponseEntity.status(s).body(m);
    }

    @ExceptionHandler(BankException.class)
    public ResponseEntity<Map<String, String>> bank(BankException e) {
        return body(e.getStatus(), e.getMessage(), e.getCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream().findFirst()
                .map(f -> f.getDefaultMessage()).orElse("Invalid input");
        return body(HttpStatus.BAD_REQUEST, msg, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException e) {
        return body(HttpStatus.BAD_REQUEST, "Malformed request", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> other(Exception e) {
        e.printStackTrace();
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error. Please try again.", null);
    }
}
