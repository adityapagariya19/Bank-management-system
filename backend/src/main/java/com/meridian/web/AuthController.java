package com.meridian.web;

import com.meridian.dto.Dtos.*;
import com.meridian.model.OtpPurpose;
import com.meridian.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public MessageDto register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }

    @PostMapping("/register/verify")
    public AuthResponse verifyRegistration(@Valid @RequestBody OtpVerifyRequest r) { return auth.verifyRegistration(r); }

    @PostMapping("/register/resend")
    public MessageDto resendRegistration(@Valid @RequestBody IdentifierRequest r) { return auth.resendOtp(r, OtpPurpose.REGISTER); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/login/otp/request")
    public MessageDto requestLoginOtp(@Valid @RequestBody IdentifierRequest r) { return auth.resendOtp(r, OtpPurpose.LOGIN); }

    @PostMapping("/login/otp/verify")
    public AuthResponse loginWithOtp(@Valid @RequestBody OtpVerifyRequest r) { return auth.loginWithOtp(r); }

    @PostMapping("/password/forgot")
    public MessageDto forgot(@Valid @RequestBody IdentifierRequest r) { return auth.requestPasswordReset(r); }

    @PostMapping("/password/reset")
    public MessageDto reset(@Valid @RequestBody ResetPasswordRequest r) { return auth.resetPassword(r); }
}
