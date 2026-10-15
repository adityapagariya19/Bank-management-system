package com.meridian.service;

import com.meridian.dto.Dtos.*;
import com.meridian.model.*;
import com.meridian.repo.*;
import com.meridian.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Teller, manager, auditor and admin operations. */
@Service
@Transactional(noRollbackFor = PinException.class)
public class StaffService {
    private static final BigDecimal MAX_CASH_DEPOSIT = new BigDecimal("1000000");
    private static final BigDecimal MAX_CASH_WITHDRAWAL = new BigDecimal("200000");

    private final UserRepository users;
    private final KycRepository kycs;
    private final BankRepository banks;
    private final BankAccountRepository accounts;
    private final TransactionRepository txs;
    private final AuditLogRepository auditLogs;
    private final PasswordEncoder encoder;
    private final PinService pins;
    private final LedgerService ledger;
    private final AuditService audit;
    private final NotificationService notify;

    public StaffService(UserRepository users, KycRepository kycs, BankRepository banks, BankAccountRepository accounts,
                        TransactionRepository txs, AuditLogRepository auditLogs, PasswordEncoder encoder, PinService pins,
                        LedgerService ledger, AuditService audit, NotificationService notify) {
        this.users = users; this.kycs = kycs; this.banks = banks; this.accounts = accounts; this.txs = txs;
        this.auditLogs = auditLogs; this.encoder = encoder; this.pins = pins; this.ledger = ledger; this.audit = audit; this.notify = notify;
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> new BankException("User not found", HttpStatus.NOT_FOUND));
    }

    // ================================================================ TELLER
    @Transactional(readOnly = true)
    public AccountLookupDto lookup(String accountNumber) {
        BankAccount a = accounts.findByAccountNumber(accountNumber == null ? "" : accountNumber.trim())
                .orElseThrow(() -> new BankException("No account found with this number", HttpStatus.NOT_FOUND));
        return new AccountLookupDto(a.getId(), a.getAccountNumber(), a.getOwner().getFullName(), a.getBank().getName(),
                a.getBranch().getName(), a.getBranch().getIfsc(), a.getType().name(), a.getStatus().name(), a.getBalance());
    }

    public TransactionDto deposit(AuthUser me, CounterRequest r) {
        BigDecimal amount = r.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(MAX_CASH_DEPOSIT) > 0) throw new BankException("Cash deposits above " + Mappers.inr(MAX_CASH_DEPOSIT) + " need manager approval");
        BankAccount a = lockByNumber(r.accountNumber());
        if (a.getStatus() != AccountStatus.ACTIVE) throw new BankException("Account is " + a.getStatus().name().toLowerCase() + ". Deposit not allowed.");
        a.setBalance(a.getBalance().add(amount));
        BankTransaction t = ledger.record(TxType.CASH_DEPOSIT, TxChannel.COUNTER, null, a, amount, null, null,
                r.remark() == null || r.remark().isBlank() ? "Cash deposit at counter" : r.remark(), user(me.id()));
        notify.push(a.getOwner(), "Cash deposited", Mappers.inr(amount) + " deposited to your " + a.getBank().getShortName() + " account " + Mappers.maskAcc(a.getAccountNumber()), "CREDIT");
        audit.log(me.id(), me.email(), "CASH_DEPOSIT", "BANK_ACCOUNT", String.valueOf(a.getId()), Mappers.inr(amount) + " to " + Mappers.maskAcc(a.getAccountNumber()), true);
        return Mappers.tx(t, Set.of(a.getId()), a.getId());
    }

    public TransactionDto withdraw(AuthUser me, CounterRequest r) {
        BigDecimal amount = r.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(MAX_CASH_WITHDRAWAL) > 0) throw new BankException("Counter withdrawal limit is " + Mappers.inr(MAX_CASH_WITHDRAWAL));
        BankAccount a = lockByNumber(r.accountNumber());
        if (a.getStatus() != AccountStatus.ACTIVE) throw new BankException("Account is " + a.getStatus().name().toLowerCase() + ". Withdrawal not allowed.");
        pins.verify(a, r.pin(), me.id(), me.email());
        if (a.getBalance().compareTo(amount) < 0) throw new BankException("Insufficient balance");
        a.setBalance(a.getBalance().subtract(amount));
        BankTransaction t = ledger.record(TxType.CASH_WITHDRAWAL, TxChannel.COUNTER, a, null, amount, null, null,
                r.remark() == null || r.remark().isBlank() ? "Cash withdrawal at counter" : r.remark(), user(me.id()));
        notify.push(a.getOwner(), "Cash withdrawn", Mappers.inr(amount) + " withdrawn from your " + a.getBank().getShortName() + " account " + Mappers.maskAcc(a.getAccountNumber()), "DEBIT");
        audit.log(me.id(), me.email(), "CASH_WITHDRAWAL", "BANK_ACCOUNT", String.valueOf(a.getId()), Mappers.inr(amount) + " from " + Mappers.maskAcc(a.getAccountNumber()), true);
        return Mappers.tx(t, Set.of(a.getId()), a.getId());
    }

    private BankAccount lockByNumber(String number) {
        Long id = accounts.findByAccountNumber(number == null ? "" : number.trim())
                .orElseThrow(() -> new BankException("No account found with this number", HttpStatus.NOT_FOUND)).getId();
        return accounts.findByIdForUpdate(id).orElseThrow(() -> new BankException("Account not found", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> recentCounter(AuthUser me) {
        return txs.findByPerformedById(me.id(), PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .getContent().stream().map(t -> Mappers.tx(t, Set.of(), null)).toList();
    }

    // ================================================================ MANAGER
    @Transactional(readOnly = true)
    public ManagerOverview overview() {
        List<BankSummary> list = accounts.bankSummary().stream()
                .map(o -> new BankSummary((String) o[0], (String) o[1], ((Number) o[2]).longValue(), new BigDecimal(o[3].toString()))).toList();
        return new ManagerOverview(kycs.countByStatus(KycStatus.PENDING), users.countByRole(Role.CUSTOMER), accounts.count(),
                accounts.countByStatus(AccountStatus.FROZEN), accounts.totalBalance(),
                txs.countByCreatedAtAfter(LocalDate.now().atStartOfDay()), list);
    }

    @Transactional(readOnly = true)
    public List<KycDto> kycList(String status) {
        KycStatus s;
        try {
            s = KycStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            s = KycStatus.PENDING;
        }
        if (s == KycStatus.PENDING) return kycs.findByStatusOrderBySubmittedAtAsc(s).stream().map(Mappers::kyc).toList();
        final KycStatus fs = s;
        return kycs.findAll().stream().filter(k -> k.getStatus() == fs)
                .sorted((a, b) -> b.getReviewedAt() == null ? -1 : a.getReviewedAt() == null ? 1 : b.getReviewedAt().compareTo(a.getReviewedAt()))
                .limit(100).map(Mappers::kyc).toList();
    }

    public KycDto reviewKyc(AuthUser me, Long kycId, KycReviewRequest r) {
        KycProfile k = kycs.findById(kycId).orElseThrow(() -> new BankException("KYC record not found", HttpStatus.NOT_FOUND));
        if (k.getStatus() != KycStatus.PENDING) throw new BankException("This KYC has already been reviewed");
        if (!r.approve() && (r.note() == null || r.note().isBlank())) throw new BankException("Please give a reason for rejection");
        k.setStatus(r.approve() ? KycStatus.APPROVED : KycStatus.REJECTED);
        k.setReviewNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        k.setReviewedBy(user(me.id()));
        k.setReviewedAt(LocalDateTime.now());
        if (r.approve()) notify.push(k.getUser(), "KYC approved", "You can now link a bank account and start paying.", "SUCCESS");
        else notify.push(k.getUser(), "KYC rejected", "Reason: " + k.getReviewNote() + ". Please resubmit your details.", "WARNING");
        audit.log(me.id(), me.email(), r.approve() ? "KYC_APPROVED" : "KYC_REJECTED", "KYC", String.valueOf(k.getId()), k.getUser().getEmail(), true);
        return Mappers.kyc(k);
    }

    @Transactional(readOnly = true)
    public List<AdminAccountDto> accountList(String q) {
        String s = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return accounts.findAll(Sort.by(Sort.Direction.DESC, "openedAt")).stream()
                .filter(a -> s.isEmpty() || a.getAccountNumber().contains(s) || a.getOwner().getFullName().toLowerCase().contains(s)
                        || a.getOwner().getEmail().toLowerCase().contains(s) || a.getBank().getName().toLowerCase().contains(s))
                .limit(200).map(Mappers::adminAccount).toList();
    }

    public AdminAccountDto setAccountStatus(AuthUser me, Long id, String value) {
        BankAccount a = accounts.findByIdForUpdate(id).orElseThrow(() -> new BankException("Account not found", HttpStatus.NOT_FOUND));
        AccountStatus s;
        try {
            s = AccountStatus.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new BankException("Invalid status");
        }
        if (s == AccountStatus.CLOSED && a.getBalance().signum() != 0) throw new BankException("Balance must be zero before closing an account");
        if (a.getStatus() == AccountStatus.CLOSED) throw new BankException("Closed accounts cannot be modified");
        AccountStatus old = a.getStatus();
        a.setStatus(s);
        notify.push(a.getOwner(), "Account " + s.name().toLowerCase(), "Your " + a.getBank().getShortName() + " account " + Mappers.maskAcc(a.getAccountNumber()) + " is now " + s.name().toLowerCase() + ".", s == AccountStatus.ACTIVE ? "SUCCESS" : "WARNING");
        audit.log(me.id(), me.email(), "ACCOUNT_STATUS", "BANK_ACCOUNT", String.valueOf(a.getId()), old + " -> " + s, true);
        return Mappers.adminAccount(a);
    }

    // ================================================================ AUDIT
    @Transactional(readOnly = true)
    public PageDto<AuditDto> logs(String q, int page, int size) {
        Page<AuditLog> p = auditLogs.search(q == null ? "" : q.trim(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return new PageDto<>(p.getContent().stream().map(Mappers::audit).toList(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageDto<TransactionDto> allTransactions(int page, int size) {
        Page<BankTransaction> p = txs.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new PageDto<>(p.getContent().stream().map(t -> Mappers.tx(t, Set.of(), null)).toList(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    // ================================================================ ADMIN
    @Transactional(readOnly = true)
    public AdminStats stats() {
        long staff = users.count() - users.countByRole(Role.CUSTOMER);
        return new AdminStats(users.count(), users.countByRole(Role.CUSTOMER), staff, accounts.count(), banks.count(), txs.count(),
                accounts.totalBalance(), users.countLocked(LocalDateTime.now()), txs.countByCreatedAtAfter(LocalDate.now().atStartOfDay()));
    }

    private AdminUserDto adminUser(User u) {
        String kyc = u.getRole() == Role.CUSTOMER ? kycs.findByUserId(u.getId()).map(k -> k.getStatus().name()).orElse("NOT_SUBMITTED") : null;
        boolean locked = u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now());
        return new AdminUserDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole().name(), u.getStatus().name(), kyc,
                u.getFailedAttempts(), locked, u.getCreatedAt(), u.getLastLoginAt());
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> userList(String q, String role) {
        String s = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return users.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(u -> role == null || role.isBlank() || u.getRole().name().equals(role))
                .filter(u -> s.isEmpty() || u.getFullName().toLowerCase().contains(s) || u.getEmail().contains(s) || u.getPhone().contains(s))
                .limit(300).map(this::adminUser).toList();
    }

    public AdminUserDto setUserStatus(AuthUser me, Long id, String value) {
        User u = user(id);
        if (u.getId().equals(me.id())) throw new BankException("You cannot change your own status");
        UserStatus s;
        try {
            s = UserStatus.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new BankException("Invalid status");
        }
        if (s == UserStatus.PENDING_VERIFICATION) throw new BankException("Invalid status");
        u.setStatus(s);
        audit.log(me.id(), me.email(), "USER_STATUS", "USER", String.valueOf(u.getId()), u.getEmail() + " -> " + s, true);
        return adminUser(u);
    }

    public AdminUserDto unlock(AuthUser me, Long id) {
        User u = user(id);
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        audit.log(me.id(), me.email(), "USER_UNLOCKED", "USER", String.valueOf(u.getId()), u.getEmail(), true);
        return adminUser(u);
    }

    public AdminUserDto createStaff(AuthUser me, CreateStaffRequest r) {
        Role role;
        try {
            role = Role.valueOf(r.role().toUpperCase());
        } catch (Exception e) {
            throw new BankException("Unknown role");
        }
        if (role == Role.CUSTOMER) throw new BankException("Use the registration flow for customers");
        AuthService.validatePassword(r.password());
        String email = r.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent() || users.findByPhone(r.phone()).isPresent())
            throw new BankException("Email or mobile already in use", HttpStatus.CONFLICT);
        User u = new User();
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPhone(r.phone());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        users.save(u);
        audit.log(me.id(), me.email(), "STAFF_CREATED", "USER", String.valueOf(u.getId()), email + " as " + role, true);
        return adminUser(u);
    }
}
