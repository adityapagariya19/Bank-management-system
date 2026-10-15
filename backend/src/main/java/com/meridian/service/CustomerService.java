package com.meridian.service;

import com.meridian.dto.Dtos.*;
import com.meridian.model.*;
import com.meridian.repo.*;
import com.meridian.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** KYC, banks, bank accounts, PIN management, UPI IDs, contacts, dashboard and notifications for customers. */
@Service
@Transactional(noRollbackFor = PinException.class)
public class CustomerService {
    private static final LocalDateTime MIN = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime MAX = LocalDateTime.of(2999, 1, 1, 0, 0);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");

    private final UserRepository users;
    private final KycRepository kycs;
    private final BankRepository banks;
    private final BranchRepository branches;
    private final BankAccountRepository accounts;
    private final UpiRepository upis;
    private final BeneficiaryRepository bens;
    private final NotificationRepository notifs;
    private final TransactionRepository txs;
    private final PaymentRequestRepository requests;
    private final AuditLogRepository auditLogs;
    private final PinService pins;
    private final OtpService otp;
    private final AuditService audit;
    private final NotificationService notify;
    private final UpiLookup lookup;
    private final SecureRandom rnd = new SecureRandom();

    public CustomerService(UserRepository users, KycRepository kycs, BankRepository banks, BranchRepository branches,
                           BankAccountRepository accounts, UpiRepository upis, BeneficiaryRepository bens,
                           NotificationRepository notifs, TransactionRepository txs, PaymentRequestRepository requests,
                           AuditLogRepository auditLogs, PinService pins, OtpService otp, AuditService audit,
                           NotificationService notify, UpiLookup lookup) {
        this.users = users; this.kycs = kycs; this.banks = banks; this.branches = branches; this.accounts = accounts;
        this.upis = upis; this.bens = bens; this.notifs = notifs; this.txs = txs; this.requests = requests;
        this.auditLogs = auditLogs; this.pins = pins; this.otp = otp; this.audit = audit; this.notify = notify; this.lookup = lookup;
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> new BankException("User not found", HttpStatus.NOT_FOUND));
    }

    private BankAccount owned(Long userId, Long accountId) {
        BankAccount a = accounts.findById(accountId).orElseThrow(() -> new BankException("Account not found", HttpStatus.NOT_FOUND));
        if (!a.getOwner().getId().equals(userId)) throw new BankException("This is not your account", HttpStatus.FORBIDDEN);
        return a;
    }

    // ------------------------------------------------------------ KYC
    private KycProfile kycOf(Long userId) {
        return kycs.findByUserId(userId).orElseGet(() -> {
            KycProfile k = new KycProfile();
            k.setUser(user(userId));
            return kycs.save(k);
        });
    }

    public KycDto kyc(AuthUser me) {
        return Mappers.kyc(kycOf(me.id()));
    }

    public KycDto submitKyc(AuthUser me, KycRequest r) {
        KycProfile k = kycOf(me.id());
        if (k.getStatus() == KycStatus.PENDING) throw new BankException("Your KYC is already under review");
        if (k.getStatus() == KycStatus.APPROVED) throw new BankException("Your KYC is already verified");
        if (Period.between(r.dateOfBirth(), LocalDate.now()).getYears() < 18) throw new BankException("You must be at least 18 years old");
        if (r.dateOfBirth().isAfter(LocalDate.now())) throw new BankException("Date of birth cannot be in the future");
        String id = r.idNumber().trim().toUpperCase().replace(" ", "");
        boolean ok = switch (r.idType()) {
            case "AADHAAR" -> id.matches("\\d{12}");
            case "PAN" -> id.matches("[A-Z]{5}\\d{4}[A-Z]");
            case "PASSPORT" -> id.matches("[A-Z]\\d{7}");
            case "VOTER_ID" -> id.matches("[A-Z]{3}\\d{7}");
            default -> throw new BankException("Unsupported ID type");
        };
        if (!ok) throw new BankException("Invalid " + r.idType().replace('_', ' ') + " number format");
        k.setDateOfBirth(r.dateOfBirth());
        k.setAddress(r.address().trim());
        k.setCity(r.city().trim());
        k.setState(r.state().trim());
        k.setPincode(r.pincode());
        k.setIdType(r.idType());
        k.setIdNumberMasked("X".repeat(id.length() - 4) + id.substring(id.length() - 4));
        k.setStatus(KycStatus.PENDING);
        k.setSubmittedAt(LocalDateTime.now());
        k.setReviewNote(null);
        k.setReviewedBy(null);
        k.setReviewedAt(null);
        kycs.save(k);
        audit.log(me.id(), me.email(), "KYC_SUBMITTED", "KYC", String.valueOf(k.getId()), r.idType() + " " + k.getIdNumberMasked(), true);
        return Mappers.kyc(k);
    }

    // ------------------------------------------------------------ banks
    @Transactional(readOnly = true)
    public List<BankDto> banks() {
        return banks.findByActiveTrueOrderByNameAsc().stream().map(b -> new BankDto(b.getId(), b.getCode(), b.getName(),
                b.getShortName(), b.getColor(), b.getHeadquarters(),
                branches.findByBankIdOrderByCityAsc(b.getId()).stream()
                        .map(x -> new BranchDto(x.getId(), x.getName(), x.getIfsc(), x.getCity(), x.getState())).toList())).toList();
    }

    // ------------------------------------------------------------ accounts
    @Transactional(readOnly = true)
    public List<AccountDto> accounts(AuthUser me) {
        return accounts.findByOwnerId(me.id()).stream().filter(a -> a.getStatus() != AccountStatus.CLOSED)
                .sorted(Comparator.comparing(BankAccount::isPrimaryAccount).reversed().thenComparing(BankAccount::getId))
                .map(Mappers::account).toList();
    }

    public AccountDto openAccount(AuthUser me, OpenAccountRequest r) {
        if (kycOf(me.id()).getStatus() != KycStatus.APPROVED)
            throw new BankException("Complete KYC verification before linking a bank account.", HttpStatus.FORBIDDEN, "KYC_REQUIRED");
        PinService.validateNew(r.pin());
        Bank bank = banks.findById(r.bankId()).filter(Bank::isActive).orElseThrow(() -> new BankException("Bank not found", HttpStatus.NOT_FOUND));
        Branch branch = branches.findById(r.branchId()).filter(b -> b.getBank().getId().equals(bank.getId()))
                .orElseThrow(() -> new BankException("Branch does not belong to the selected bank"));
        AccountType type;
        try {
            type = AccountType.valueOf(r.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BankException("Unknown account type");
        }
        List<BankAccount> mine = accounts.findByOwnerId(me.id()).stream().filter(a -> a.getStatus() != AccountStatus.CLOSED).toList();
        if (mine.size() >= 6) throw new BankException("You can link a maximum of 6 bank accounts");
        if (mine.stream().anyMatch(a -> a.getBank().getId().equals(bank.getId()) && a.getType() == type))
            throw new BankException("You already have a " + type.name().toLowerCase() + " account with " + bank.getShortName());

        User u = user(me.id());
        BankAccount a = new BankAccount();
        String num;
        do {
            num = String.valueOf(100_000_000_000L + Math.floorMod(rnd.nextLong(), 900_000_000_000L));
        } while (accounts.findByAccountNumber(num).isPresent());
        a.setAccountNumber(num);
        a.setBank(bank);
        a.setBranch(branch);
        a.setOwner(u);
        a.setType(type);
        a.setPinHash(pins.hash(r.pin()));
        a.setPrimaryAccount(mine.isEmpty());
        accounts.save(a);

        if (upis.findByUserIdAndPrimaryVpaTrue(u.getId()).isEmpty()) {
            UpiId upi = new UpiId();
            upi.setVpa(u.getPhone() + UpiLookup.HANDLE);
            upi.setUser(u);
            upi.setAccount(a);
            upi.setPrimaryVpa(true);
            upis.save(upi);
        }
        notify.push(u, "Bank account linked", bank.getShortName() + " " + type.name().toLowerCase() + " account " + Mappers.maskAcc(num) + " is ready. Visit a branch counter to deposit cash.", "SUCCESS");
        audit.log(me.id(), me.email(), "ACCOUNT_OPENED", "BANK_ACCOUNT", String.valueOf(a.getId()), bank.getCode() + " " + type + " " + Mappers.maskAcc(num), true);
        return Mappers.account(a);
    }

    public MessageDto setPrimary(AuthUser me, Long accountId) {
        BankAccount target = owned(me.id(), accountId);
        if (target.getStatus() != AccountStatus.ACTIVE) throw new BankException("Only active accounts can be primary");
        accounts.findByOwnerId(me.id()).forEach(a -> a.setPrimaryAccount(a.getId().equals(target.getId())));
        return new MessageDto("Primary account updated");
    }

    // ------------------------------------------------------------ PIN management
    public MessageDto changePin(AuthUser me, Long accountId, ChangePinRequest r) {
        BankAccount a = owned(me.id(), accountId);
        PinService.validateNew(r.newPin());
        pins.verify(a, r.oldPin(), me.id(), me.email());
        a.setPinHash(pins.hash(r.newPin()));
        audit.log(me.id(), me.email(), "PIN_CHANGED", "BANK_ACCOUNT", String.valueOf(a.getId()), "PIN changed", true);
        notify.push(a.getOwner(), "PIN changed", "The PIN for " + a.getBank().getShortName() + " " + Mappers.maskAcc(a.getAccountNumber()) + " was changed.", "SECURITY");
        return new MessageDto("PIN changed successfully");
    }

    public MessageDto requestPinReset(AuthUser me, Long accountId) {
        owned(me.id(), accountId);
        otp.issue(user(me.id()), OtpPurpose.RESET_PIN);
        return new MessageDto("A code has been sent to your registered mobile and email");
    }

    public MessageDto resetPin(AuthUser me, Long accountId, ResetPinRequest r) {
        BankAccount a = owned(me.id(), accountId);
        PinService.validateNew(r.newPin());
        otp.verify(user(me.id()), OtpPurpose.RESET_PIN, r.code());
        a.setPinHash(pins.hash(r.newPin()));
        a.setPinAttempts(0);
        a.setPinLockedUntil(null);
        audit.log(me.id(), me.email(), "PIN_RESET", "BANK_ACCOUNT", String.valueOf(a.getId()), "PIN reset via OTP", true);
        return new MessageDto("PIN reset successfully");
    }

    // ------------------------------------------------------------ UPI IDs
    private UpiDto upiDto(UpiId u) {
        return new UpiDto(u.getId(), u.getVpa(), u.getAccount().getId(), u.getAccount().getBank().getShortName(),
                Mappers.maskAcc(u.getAccount().getAccountNumber()), u.isPrimaryVpa());
    }

    @Transactional(readOnly = true)
    public List<UpiDto> upiIds(AuthUser me) {
        return upis.findByUserIdOrderByPrimaryVpaDescIdAsc(me.id()).stream().map(this::upiDto).toList();
    }

    public UpiDto createUpi(AuthUser me, CreateUpiRequest r) {
        String h = r.handle().trim().toLowerCase();
        if (!h.matches("[a-z0-9][a-z0-9._-]{2,29}")) throw new BankException("Handle must be 3-30 characters: letters, numbers, dot, dash or underscore");
        String vpa = h + UpiLookup.HANDLE;
        if (upis.existsByVpa(vpa)) throw new BankException("That UPI ID is already taken", HttpStatus.CONFLICT);
        BankAccount a = owned(me.id(), r.accountId());
        if (a.getStatus() != AccountStatus.ACTIVE) throw new BankException("Select an active account");
        UpiId u = new UpiId();
        u.setVpa(vpa);
        u.setUser(user(me.id()));
        u.setAccount(a);
        upis.save(u);
        audit.log(me.id(), me.email(), "UPI_CREATED", "UPI", vpa, "UPI ID created", true);
        return upiDto(u);
    }

    public UpiDto linkUpi(AuthUser me, Long upiId, LinkUpiRequest r) {
        UpiId u = upis.findById(upiId).orElseThrow(() -> new BankException("UPI ID not found", HttpStatus.NOT_FOUND));
        if (!u.getUser().getId().equals(me.id())) throw new BankException("Not allowed", HttpStatus.FORBIDDEN);
        BankAccount a = owned(me.id(), r.accountId());
        if (a.getStatus() != AccountStatus.ACTIVE) throw new BankException("Select an active account");
        u.setAccount(a);
        audit.log(me.id(), me.email(), "UPI_RELINKED", "UPI", u.getVpa(), "Receiving account changed", true);
        return upiDto(u);
    }

    public void deleteUpi(AuthUser me, Long upiId) {
        UpiId u = upis.findById(upiId).orElseThrow(() -> new BankException("UPI ID not found", HttpStatus.NOT_FOUND));
        if (!u.getUser().getId().equals(me.id())) throw new BankException("Not allowed", HttpStatus.FORBIDDEN);
        if (u.isPrimaryVpa()) throw new BankException("Your primary UPI ID cannot be removed");
        upis.delete(u);
    }

    // ------------------------------------------------------------ payee lookup
    @Transactional(readOnly = true)
    public PayeeDto lookupVpa(String q) {
        UpiId u = lookup.find(q);
        BankAccount a = u.getAccount();
        return new PayeeDto(u.getUser().getFullName(), u.getVpa(), a.getBank().getName(), Mappers.maskAcc(a.getAccountNumber()), a.getBranch().getIfsc());
    }

    @Transactional(readOnly = true)
    public PayeeDto lookupAccount(String accountNumber, String ifsc) {
        BankAccount a = accounts.findByAccountNumberAndBranch_Ifsc(accountNumber.trim(), ifsc.trim().toUpperCase())
                .orElseThrow(() -> new BankException("No account found for this account number and IFSC", HttpStatus.NOT_FOUND));
        return new PayeeDto(a.getOwner().getFullName(), null, a.getBank().getName(), Mappers.maskAcc(a.getAccountNumber()), a.getBranch().getIfsc());
    }

    // ------------------------------------------------------------ contacts
    @Transactional(readOnly = true)
    public List<BeneficiaryDto> contacts(AuthUser me) {
        return bens.findByOwnerIdOrderByNameAsc(me.id()).stream().map(b -> new BeneficiaryDto(b.getId(), b.getName(), b.getVpa(), b.getNickname())).toList();
    }

    public BeneficiaryDto addContact(AuthUser me, BeneficiaryRequest r) {
        UpiId u = lookup.find(r.vpa());
        if (u.getUser().getId().equals(me.id())) throw new BankException("You cannot add yourself as a contact");
        if (bens.existsByOwnerIdAndVpa(me.id(), u.getVpa())) throw new BankException("Already in your contacts", HttpStatus.CONFLICT);
        Beneficiary b = new Beneficiary();
        b.setOwner(user(me.id()));
        b.setName(u.getUser().getFullName());
        b.setVpa(u.getVpa());
        b.setNickname(r.nickname() == null ? "" : r.nickname().trim());
        bens.save(b);
        return new BeneficiaryDto(b.getId(), b.getName(), b.getVpa(), b.getNickname());
    }

    public void deleteContact(AuthUser me, Long id) {
        Beneficiary b = bens.findById(id).orElseThrow(() -> new BankException("Contact not found", HttpStatus.NOT_FOUND));
        if (!b.getOwner().getId().equals(me.id())) throw new BankException("Not allowed", HttpStatus.FORBIDDEN);
        bens.delete(b);
    }

    // ------------------------------------------------------------ dashboard
    @Transactional(readOnly = true)
    public DashboardDto dashboard(AuthUser me) {
        List<BankAccount> accs = accounts.findByOwnerId(me.id()).stream().filter(a -> a.getStatus() != AccountStatus.CLOSED).toList();
        Set<Long> ids = accs.stream().map(BankAccount::getId).collect(Collectors.toSet());
        BigDecimal total = accs.stream().map(BankAccount::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BankSlice> split = new LinkedHashMap<>();
        for (BankAccount a : accs) {
            split.merge(a.getBank().getShortName(), new BankSlice(a.getBank().getShortName(), a.getBank().getColor(), a.getBalance()),
                    (x, y) -> new BankSlice(x.bank(), x.color(), x.balance().add(y.balance())));
        }

        LocalDate today = LocalDate.now();
        Map<LocalDate, BigDecimal[]> days = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) days.put(today.minusDays(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal inc = BigDecimal.ZERO, exp = BigDecimal.ZERO;
        List<TransactionDto> recent = List.of();
        if (!ids.isEmpty()) {
            for (BankTransaction t : txs.findSince(ids, today.minusDays(29).atStartOfDay())) {
                boolean fm = t.getFromAccount() != null && ids.contains(t.getFromAccount().getId());
                boolean tm = t.getToAccount() != null && ids.contains(t.getToAccount().getId());
                BigDecimal[] bucket = days.get(t.getCreatedAt().toLocalDate());
                if (tm && !fm) {
                    inc = inc.add(t.getAmount());
                    if (bucket != null) bucket[0] = bucket[0].add(t.getAmount());
                } else if (fm && !tm) {
                    exp = exp.add(t.getAmount());
                    if (bucket != null) bucket[1] = bucket[1].add(t.getAmount());
                }
            }
            recent = txs.findForAccounts(ids, MIN, MAX, PageRequest.of(0, 6)).getContent().stream().map(t -> Mappers.tx(t, ids, null)).toList();
        }
        List<DailyPoint> chart = days.entrySet().stream().map(e -> new DailyPoint(e.getKey().format(DAY), e.getValue()[0], e.getValue()[1])).toList();
        return new DashboardDto(total, inc, exp, accs.size(), kycOf(me.id()).getStatus().name(),
                requests.countByPayerIdAndStatus(me.id(), RequestStatus.PENDING), chart, new ArrayList<>(split.values()), recent);
    }

    // ------------------------------------------------------------ security activity & notifications
    @Transactional(readOnly = true)
    public List<ActivityDto> activity(AuthUser me) {
        return auditLogs.findTop10ByActorIdAndActionInOrderByCreatedAtDesc(me.id(),
                List.of("LOGIN", "LOGIN_FAILED", "ACCOUNT_LOCKED", "PASSWORD_CHANGED", "PASSWORD_RESET", "PIN_CHANGED", "PIN_RESET", "PIN_LOCKED"))
                .stream().map(a -> new ActivityDto(a.getAction(), a.isSuccess(), a.getIpAddress(), a.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public NotificationsDto notifications(Long userId) {
        return new NotificationsDto(notifs.findTop30ByUserIdOrderByCreatedAtDesc(userId).stream().map(Mappers::notification).toList(),
                notifs.countByUserIdAndSeenFalse(userId));
    }

    public void markAllRead(Long userId) {
        notifs.markAllSeen(userId);
    }
}
