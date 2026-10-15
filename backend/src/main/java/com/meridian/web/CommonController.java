package com.meridian.web;

import com.meridian.dto.Dtos.*;
import com.meridian.security.AuthUser;
import com.meridian.service.AuthService;
import com.meridian.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Endpoints available to every signed-in user (any role). */
@RestController
@RequestMapping("/api")
public class CommonController {
    private final AuthService auth;
    private final CustomerService customers;

    public CommonController(AuthService auth, CustomerService customers) {
        this.auth = auth;
        this.customers = customers;
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthUser me) { return auth.me(me.id()); }

    @PostMapping("/me/password")
    public MessageDto changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangePasswordRequest r) {
        return auth.changePassword(me.id(), r);
    }

    @GetMapping("/notifications")
    public NotificationsDto notifications(@AuthenticationPrincipal AuthUser me) { return customers.notifications(me.id()); }

    @PostMapping("/notifications/read-all")
    public MessageDto readAll(@AuthenticationPrincipal AuthUser me) {
        customers.markAllRead(me.id());
        return new MessageDto("ok");
    }
}
