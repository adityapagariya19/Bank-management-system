package com.meridian.service;

import com.meridian.model.UpiId;
import com.meridian.model.User;
import com.meridian.repo.UpiRepository;
import com.meridian.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Resolves "9876543210", "aarav" or "aarav@meridian" to a UPI ID record. */
@Component
public class UpiLookup {
    public static final String HANDLE = "@meridian";

    private final UpiRepository upis;
    private final UserRepository users;

    public UpiLookup(UpiRepository upis, UserRepository users) {
        this.upis = upis;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UpiId find(String query) {
        String v = query == null ? "" : query.trim().toLowerCase();
        if (v.isEmpty()) throw new BankException("Enter a UPI ID or mobile number");
        if (v.matches("\\d{10}")) {
            User u = users.findByPhone(v).orElseThrow(() -> new BankException("No Meridian user found with this mobile number", HttpStatus.NOT_FOUND));
            return upis.findByUserIdAndPrimaryVpaTrue(u.getId())
                    .orElseThrow(() -> new BankException("This user has not linked a bank account yet", HttpStatus.NOT_FOUND));
        }
        if (!v.contains("@")) v = v + HANDLE;
        return upis.findByVpa(v).orElseThrow(() -> new BankException("UPI ID not found", HttpStatus.NOT_FOUND));
    }
}
