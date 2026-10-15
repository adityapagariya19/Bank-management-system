package com.meridian.service;

import com.meridian.dto.Dtos.*;
import com.meridian.model.*;
import com.meridian.repo.KycRepository;
import com.meridian.repo.UserRepository;
import com.meridian.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Registration with OTP verification, password login, OTP login, password reset and password change.
 * The whole class commits on BankException so failed-attempt counters and OTP attempt counters are persisted.
 */
@Service
@Transactional(noRollbackFor = BankException.class)
public class AuthService {
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$");
    private static final int MAX_FAILED = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository users;
    private final KycRepository kycs;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final OtpService otp;
    private final AuditService audit;
    private final NotificationService notify;

    public AuthService(UserRepository users, KycRepository kycs, PasswordEncoder encoder, JwtService jwt,
                       OtpService otp, AuditService audit, NotificationService notify) {
        this.users = users;
        this.kycs = kycs;
        this.encoder = encoder;
        this.jwt = jwt;
        this.otp = otp;
        this.audit = audit;
        this.notify = notify;
    }

    // ------------------------------------------------------------ helpers
    public static void validatePassword(String p) {
        if (p == null || !PASSWORD.matcher(p).matches())
            throw new BankException("Password must be at least 8 characters and contain letters and numbers");
    }

    public Optional<User> find(String identifier) {
        String v = identifier == null ? "" : identifier.trim().toLowerCase();
        if (v.contains("@")) return users.findByEmail(v);
        String d = v.replaceAll("\\D", "");
        if (d.length() > 10) d = d.substring(d.length() - 10);
        return users.findByPhone(d);
    }

    private User require(String identifier) {
        return find(identifier).orElseThrow(() -> new BankException("No account found with these details", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserDto toDto(User u) {
        String kyc = u.getRole() == Role.CUSTOMER
                ? kycs.findByUserId(u.getId()).map(k -> k.getStatus().name()).orElse("NOT_SUBMITTED") : null;
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole().name(), u.getStatus().name(),
                kyc, u.getCreatedAt(), u.getLastLoginAt());
    }

    private static String maskEmail(String e) {
        int at = e.indexOf('@');
        return e.charAt(0) + "***" + e.substring(at);
    }

    private static String maskPhone(String p) {
        return "XXXXXX" + p.substring(p.length() - 4);
    }

    private AuthResponse session(User u, String method) {
        u.setLastLoginAt(LocalDateTime.now());
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        users.save(u);
        audit.log(u.getId(), u.getEmail(), "LOGIN", "USER", String.valueOf(u.getId()), "Signed in using " + method, true);
        return new AuthResponse(jwt.generate(u.getId(), u.getRole().name()), toDto(u));
    }

    private void ensureCanSignIn(User u) {
        if (u.getStatus() == UserStatus.PENDING_VERIFICATION)
            throw new BankException("Please verify your account with the code we sent you.", HttpStatus.FORBIDDEN, "NEEDS_VERIFICATION");
        if (u.getStatus() == UserStatus.DISABLED)
            throw new BankException("This account has been disabled. Please contact the bank.", HttpStatus.FORBIDDEN, "DISABLED");
        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now())) {
            long mins = Math.max(1, Duration.between(LocalDateTime.now(), u.getLockedUntil()).toMinutes());
            throw new BankException("Too many failed attempts. Try again in " + mins + " minute(s).", HttpStatus.LOCKED, "LOCKED");
        }
    }

    // ------------------------------------------------------------ registration
    public MessageDto register(RegisterRequest r) {
        validatePassword(r.password());
        String email = r.email().trim().toLowerCase();
        Optional<User> byEmail = users.findByEmail(email);
        Optional<User> byPhone = users.findByPhone(r.phone());
        User u = byEmail.orElse(null);
        if (byPhone.isPresent()) {
            if (u != null && !u.getId().equals(byPhone.get().getId()))
                throw new BankException("Email or mobile number is already registered", HttpStatus.CONFLICT);
            u = byPhone.get();
        }
        if (u != null && u.getStatus() != UserStatus.PENDING_VERIFICATION)
            throw new BankException("An account with this email or mobile already exists. Please sign in.", HttpStatus.CONFLICT);
        if (u == null) {
            u = new User();
            u.setRole(Role.CUSTOMER);
        }
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPhone(r.phone());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setStatus(UserStatus.PENDING_VERIFICATION);
        users.save(u);
        otp.issue(u, OtpPurpose.REGISTER);
        return new MessageDto("Verification code sent to " + maskEmail(email) + " and " + maskPhone(r.phone()));
    }

    public AuthResponse verifyRegistration(OtpVerifyRequest r) {
        User u = require(r.identifier());
        if (u.getStatus() != UserStatus.PENDING_VERIFICATION)
            throw new BankException("This account is already verified. Please sign in.");
        otp.verify(u, OtpPurpose.REGISTER, r.code());
        u.setStatus(UserStatus.ACTIVE);
        users.save(u);
        KycProfile k = new KycProfile();
        k.setUser(u);
        kycs.save(k);
        notify.push(u, "Welcome to Meridian", "Your account is verified. Complete KYC to link your first bank account.", "INFO");
        audit.log(u.getId(), u.getEmail(), "REGISTER", "USER", String.valueOf(u.getId()), "New customer verified", true);
        return session(u, "registration OTP");
    }

    public MessageDto resendOtp(IdentifierRequest r, OtpPurpose purpose) {
        User u = require(r.identifier());
        if (purpose == OtpPurpose.REGISTER && u.getStatus() != UserStatus.PENDING_VERIFICATION)
            throw new BankException("This account is already verified.");
        if (purpose == OtpPurpose.LOGIN) ensureCanSignIn(u);
        otp.issue(u, purpose);
        return new MessageDto("A new code has been sent to " + maskEmail(u.getEmail()) + " and " + maskPhone(u.getPhone()));
    }

    // ------------------------------------------------------------ sign-in
    public AuthResponse login(LoginRequest r) {
        User u = find(r.identifier()).orElseThrow(() -> new BankException("Invalid credentials", HttpStatus.UNAUTHORIZED));
        ensureCanSignIn(u);
        if (!encoder.matches(r.password(), u.getPasswordHash())) {
            u.setFailedAttempts(u.getFailedAttempts() + 1);
            int left = MAX_FAILED - u.getFailedAttempts();
            if (left <= 0) {
                u.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                u.setFailedAttempts(0);
                users.save(u);
                audit.logIndependent(u.getId(), u.getEmail(), "ACCOUNT_LOCKED", "USER", String.valueOf(u.getId()), "Locked after 5 failed sign-ins", false);
                throw new BankException("Too many failed attempts. Account locked for " + LOCK_MINUTES + " minutes.", HttpStatus.LOCKED, "LOCKED");
            }
            users.save(u);
            audit.logIndependent(u.getId(), u.getEmail(), "LOGIN_FAILED", "USER", String.valueOf(u.getId()), "Wrong password", false);
            throw new BankException("Invalid credentials. " + left + " attempt(s) left.", HttpStatus.UNAUTHORIZED);
        }
        return session(u, "password");
    }

    public AuthResponse loginWithOtp(OtpVerifyRequest r) {
        User u = find(r.identifier()).orElseThrow(() -> new BankException("Invalid code", HttpStatus.UNAUTHORIZED));
        ensureCanSignIn(u);
        otp.verify(u, OtpPurpose.LOGIN, r.code());
        return session(u, "one-time code");
    }

    // ------------------------------------------------------------ password management
    public MessageDto requestPasswordReset(IdentifierRequest r) {
        find(r.identifier()).filter(u -> u.getStatus() == UserStatus.ACTIVE).ifPresent(u -> otp.issue(u, OtpPurpose.RESET_PASSWORD));
        return new MessageDto("If an account exists for these details, a reset code has been sent.");
    }

    public MessageDto resetPassword(ResetPasswordRequest r) {
        validatePassword(r.newPassword());
        User u = find(r.identifier()).filter(x -> x.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BankException("Invalid or expired code"));
        otp.verify(u, OtpPurpose.RESET_PASSWORD, r.code());
        u.setPasswordHash(encoder.encode(r.newPassword()));
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        users.save(u);
        audit.log(u.getId(), u.getEmail(), "PASSWORD_RESET", "USER", String.valueOf(u.getId()), "Password reset via OTP", true);
        notify.push(u, "Password changed", "Your password was reset. If this wasn't you, contact the bank immediately.", "SECURITY");
        return new MessageDto("Password updated. You can now sign in.");
    }

    public MessageDto changePassword(Long userId, ChangePasswordRequest r) {
        User u = users.findById(userId).orElseThrow(() -> new BankException("User not found", HttpStatus.NOT_FOUND));
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) throw new BankException("Current password is incorrect");
        validatePassword(r.newPassword());
        if (encoder.matches(r.newPassword(), u.getPasswordHash())) throw new BankException("New password must be different from the current one");
        u.setPasswordHash(encoder.encode(r.newPassword()));
        users.save(u);
        audit.log(u.getId(), u.getEmail(), "PASSWORD_CHANGED", "USER", String.valueOf(u.getId()), "Password changed from profile", true);
        notify.push(u, "Password changed", "Your password was changed successfully.", "SECURITY");
        return new MessageDto("Password changed successfully");
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return toDto(users.findById(userId).orElseThrow(() -> new BankException("User not found", HttpStatus.NOT_FOUND)));
    }
}
