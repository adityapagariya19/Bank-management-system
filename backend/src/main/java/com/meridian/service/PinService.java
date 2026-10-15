package com.meridian.service;

import com.meridian.model.BankAccount;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/** Per-bank-account PIN handling (like a UPI PIN): 3 wrong attempts lock the PIN for 30 minutes. */
@Service
public class PinService {
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_MINUTES = 30;
    private static final Set<String> WEAK = Set.of("1234", "4321", "123456", "654321", "123123", "121212", "000000", "0000");

    private final PasswordEncoder encoder;
    private final AuditService audit;

    public PinService(PasswordEncoder encoder, AuditService audit) {
        this.encoder = encoder;
        this.audit = audit;
    }

    public static void validateNew(String pin) {
        if (pin == null || !pin.matches("\\d{4}|\\d{6}")) throw new BankException("PIN must be 4 or 6 digits");
        if (pin.chars().distinct().count() == 1 || WEAK.contains(pin))
            throw new BankException("That PIN is too easy to guess. Choose a stronger one.");
    }

    public String hash(String pin) {
        return encoder.encode(pin);
    }

    /** Mutates the managed account entity. The calling transaction MUST commit on PinException. */
    public void verify(BankAccount a, String pin, Long actorId, String actorEmail) {
        LocalDateTime now = LocalDateTime.now();
        if (a.getPinLockedUntil() != null) {
            if (a.getPinLockedUntil().isAfter(now)) {
                long mins = Math.max(1, Duration.between(now, a.getPinLockedUntil()).toMinutes());
                throw new PinException("PIN is locked. Try again in " + mins + " min, or reset it using an OTP.");
            }
            a.setPinLockedUntil(null);
            a.setPinAttempts(0);
        }
        if (pin == null || !encoder.matches(pin, a.getPinHash())) {
            a.setPinAttempts(a.getPinAttempts() + 1);
            int left = MAX_ATTEMPTS - a.getPinAttempts();
            if (left <= 0) {
                a.setPinLockedUntil(now.plusMinutes(LOCK_MINUTES));
                a.setPinAttempts(0);
                audit.logIndependent(actorId, actorEmail, "PIN_LOCKED", "BANK_ACCOUNT", String.valueOf(a.getId()), "PIN locked after 3 wrong attempts", false);
                throw new PinException("Incorrect PIN. For your safety the PIN is locked for " + LOCK_MINUTES + " minutes.");
            }
            audit.logIndependent(actorId, actorEmail, "PIN_FAILED", "BANK_ACCOUNT", String.valueOf(a.getId()), "Wrong PIN entered", false);
            throw new PinException("Incorrect PIN. " + left + " attempt(s) left.");
        }
        a.setPinAttempts(0);
    }
}
