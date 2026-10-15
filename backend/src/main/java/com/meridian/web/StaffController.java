package com.meridian.web;

import com.meridian.dto.Dtos.*;
import com.meridian.security.AuthUser;
import com.meridian.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Teller, manager, auditor and admin endpoints. Role rules are enforced in SecurityConfig. */
@RestController
@RequestMapping("/api")
public class StaffController {
    private final StaffService svc;

    public StaffController(StaffService svc) {
        this.svc = svc;
    }

    // ---- teller
    @GetMapping("/teller/lookup") public AccountLookupDto lookup(@RequestParam String accountNumber) { return svc.lookup(accountNumber); }
    @PostMapping("/teller/deposit") public TransactionDto deposit(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CounterRequest r) { return svc.deposit(me, r); }
    @PostMapping("/teller/withdraw") public TransactionDto withdraw(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CounterRequest r) { return svc.withdraw(me, r); }
    @GetMapping("/teller/recent") public List<TransactionDto> recent(@AuthenticationPrincipal AuthUser me) { return svc.recentCounter(me); }

    // ---- manager
    @GetMapping("/manager/overview") public ManagerOverview overview() { return svc.overview(); }
    @GetMapping("/manager/kyc") public List<KycDto> kyc(@RequestParam(defaultValue = "PENDING") String status) { return svc.kycList(status); }
    @PostMapping("/manager/kyc/{id}/review") public KycDto review(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @RequestBody KycReviewRequest r) { return svc.reviewKyc(me, id, r); }
    @GetMapping("/manager/accounts") public List<AdminAccountDto> accounts(@RequestParam(defaultValue = "") String q) { return svc.accountList(q); }
    @PostMapping("/manager/accounts/{id}/status") public AdminAccountDto accountStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @RequestParam String value) { return svc.setAccountStatus(me, id, value); }

    // ---- audit
    @GetMapping("/audit/logs") public PageDto<AuditDto> logs(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return svc.logs(q, page, size); }
    @GetMapping("/audit/transactions") public PageDto<TransactionDto> transactions(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return svc.allTransactions(page, size); }

    // ---- admin
    @GetMapping("/admin/stats") public AdminStats stats() { return svc.stats(); }
    @GetMapping("/admin/users") public List<AdminUserDto> users(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String role) { return svc.userList(q, role); }
    @PostMapping("/admin/users/{id}/status") public AdminUserDto userStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @RequestParam String value) { return svc.setUserStatus(me, id, value); }
    @PostMapping("/admin/users/{id}/unlock") public AdminUserDto unlock(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return svc.unlock(me, id); }
    @PostMapping("/admin/staff") public AdminUserDto createStaff(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateStaffRequest r) { return svc.createStaff(me, r); }
}
