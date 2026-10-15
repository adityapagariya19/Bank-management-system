package com.meridian.web;

import com.meridian.dto.Dtos.*;
import com.meridian.security.AuthUser;
import com.meridian.service.CustomerService;
import com.meridian.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Everything a customer can do. All routes require the CUSTOMER role (see SecurityConfig). */
@RestController
@RequestMapping("/api/c")
public class CustomerController {
    private final CustomerService svc;
    private final PaymentService pay;

    public CustomerController(CustomerService svc, PaymentService pay) {
        this.svc = svc;
        this.pay = pay;
    }

    @GetMapping("/dashboard") public DashboardDto dashboard(@AuthenticationPrincipal AuthUser me) { return svc.dashboard(me); }

    // KYC
    @GetMapping("/kyc") public KycDto kyc(@AuthenticationPrincipal AuthUser me) { return svc.kyc(me); }
    @PostMapping("/kyc") public KycDto submitKyc(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody KycRequest r) { return svc.submitKyc(me, r); }

    // bank accounts
    @GetMapping("/banks") public List<BankDto> banks() { return svc.banks(); }
    @GetMapping("/accounts") public List<AccountDto> accounts(@AuthenticationPrincipal AuthUser me) { return svc.accounts(me); }
    @PostMapping("/accounts") public AccountDto open(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody OpenAccountRequest r) { return svc.openAccount(me, r); }
    @PostMapping("/accounts/{id}/primary") public MessageDto primary(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return svc.setPrimary(me, id); }
    @PostMapping("/accounts/{id}/pin/change") public MessageDto changePin(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody ChangePinRequest r) { return svc.changePin(me, id, r); }
    @PostMapping("/accounts/{id}/pin/otp") public MessageDto pinOtp(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return svc.requestPinReset(me, id); }
    @PostMapping("/accounts/{id}/pin/reset") public MessageDto resetPin(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody ResetPinRequest r) { return svc.resetPin(me, id, r); }

    // UPI IDs
    @GetMapping("/upi") public List<UpiDto> upi(@AuthenticationPrincipal AuthUser me) { return svc.upiIds(me); }
    @PostMapping("/upi") public UpiDto createUpi(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateUpiRequest r) { return svc.createUpi(me, r); }
    @PostMapping("/upi/{id}/account") public UpiDto linkUpi(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody LinkUpiRequest r) { return svc.linkUpi(me, id, r); }
    @DeleteMapping("/upi/{id}") public void deleteUpi(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { svc.deleteUpi(me, id); }

    // payees & contacts
    @GetMapping("/payees/lookup") public PayeeDto lookup(@RequestParam String q) { return svc.lookupVpa(q); }
    @GetMapping("/payees/lookup-account") public PayeeDto lookupAccount(@RequestParam String accountNumber, @RequestParam String ifsc) { return svc.lookupAccount(accountNumber, ifsc); }
    @GetMapping("/contacts") public List<BeneficiaryDto> contacts(@AuthenticationPrincipal AuthUser me) { return svc.contacts(me); }
    @PostMapping("/contacts") public BeneficiaryDto addContact(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody BeneficiaryRequest r) { return svc.addContact(me, r); }
    @DeleteMapping("/contacts/{id}") public void deleteContact(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { svc.deleteContact(me, id); }

    // payments
    @PostMapping("/pay") public TransactionDto pay(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PayRequest r) { return pay.pay(me, r); }

    @GetMapping("/transactions")
    public PageDto<TransactionDto> history(@AuthenticationPrincipal AuthUser me,
                                           @RequestParam(required = false) Long accountId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "15") int size) {
        return pay.history(me, accountId, from, to, page, size);
    }

    // payment requests
    @GetMapping("/requests") public List<PaymentRequestDto> requests(@AuthenticationPrincipal AuthUser me) { return pay.requests(me); }
    @PostMapping("/requests") public PaymentRequestDto createRequest(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateRequestRequest r) { return pay.createRequest(me, r); }
    @PostMapping("/requests/{id}/decline") public MessageDto decline(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return pay.decline(me, id); }
    @PostMapping("/requests/{id}/cancel") public MessageDto cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return pay.cancel(me, id); }

    @GetMapping("/security/activity") public List<ActivityDto> activity(@AuthenticationPrincipal AuthUser me) { return svc.activity(me); }
}
