package com.meridian.service;

import com.meridian.model.*;
import com.meridian.repo.OtpCodeRepository;
import com.meridian.repo.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * One-time passwords. Codes are stored BCrypt-hashed. Delivery is simulated: the SMS / e-mail text is written to
 * the message_outbox table (and printed on the server console) instead of calling an external gateway.
 */
@Service
public class OtpService {
    private static final int MAX_ATTEMPTS = 5;

    private final OtpCodeRepository otps;
    private final OutboxRepository outbox;
    private final PasswordEncoder encoder;
    private final long expiryMinutes;
    private final SecureRandom rnd = new SecureRandom();

    public OtpService(OtpCodeRepository otps, OutboxRepository outbox, PasswordEncoder encoder,
                      @Value("${app.otp.expiry-minutes}") long expiryMinutes) {
        this.otps = otps;
        this.outbox = outbox;
        this.encoder = encoder;
        this.expiryMinutes = expiryMinutes;
    }

    private static String title(OtpPurpose p) {
        return switch (p) {
            case REGISTER -> "Verify your Meridian account";
            case LOGIN -> "Your Meridian sign-in code";
            case RESET_PASSWORD -> "Reset your Meridian password";
            case RESET_PIN -> "Reset your bank account PIN";
        };
    }

    @Transactional
    public void issue(User u, OtpPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        otps.findTopByUserIdAndPurposeOrderByCreatedAtDesc(u.getId(), purpose).ifPresent(last -> {
            if (!last.isConsumed() && last.getCreatedAt().isAfter(now.minusSeconds(30)))
                throw new BankException("Please wait 30 seconds before requesting another code.", HttpStatus.TOO_MANY_REQUESTS);
        });
        String code = String.format("%06d", rnd.nextInt(1_000_000));
        OtpCode o = new OtpCode();
        o.setUser(u);
        o.setPurpose(purpose);
        o.setCodeHash(encoder.encode(code));
        o.setExpiresAt(now.plusMinutes(expiryMinutes));
        otps.save(o);

        String text = "Your Meridian code is " + code + ". It is valid for " + expiryMinutes
                + " minutes. Never share this code with anyone, including bank staff.";
        save("SMS", u.getPhone(), null, text);
        save("EMAIL", u.getEmail(), title(purpose), text);
        System.out.println("[SIMULATED SMS -> " + u.getPhone() + "] " + text);
    }

    private void save(String channel, String recipient, String subject, String body) {
        OutboxMessage m = new OutboxMessage();
        m.setChannel(channel);
        m.setRecipient(recipient);
        m.setSubject(subject);
        m.setBody(body);
        outbox.save(m);
    }

    /** Callers must run in a transaction that does not roll back on BankException so the attempt counter persists. */
    @Transactional(noRollbackFor = BankException.class)
    public void verify(User u, OtpPurpose purpose, String code) {
        OtpCode o = otps.findTopByUserIdAndPurposeOrderByCreatedAtDesc(u.getId(), purpose)
                .filter(x -> !x.isConsumed())
                .orElseThrow(() -> new BankException("No active code. Please request a new one."));
        if (o.getExpiresAt().isBefore(LocalDateTime.now())) throw new BankException("This code has expired. Please request a new one.");
        if (o.getAttempts() >= MAX_ATTEMPTS) throw new BankException("Too many wrong attempts. Please request a new code.");
        if (!encoder.matches(code, o.getCodeHash())) {
            o.setAttempts(o.getAttempts() + 1);
            otps.save(o);
            int left = MAX_ATTEMPTS - o.getAttempts();
            throw new BankException(left > 0 ? "Incorrect code. " + left + " attempt(s) left." : "Too many wrong attempts. Please request a new code.");
        }
        o.setConsumed(true);
        otps.save(o);
    }
}
