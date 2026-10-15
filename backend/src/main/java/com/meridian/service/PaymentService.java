package com.meridian.service;

import com.meridian.dto.Dtos.*;
import com.meridian.model.*;
import com.meridian.repo.*;
import com.meridian.security.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Money movement between customers (UPI ID, QR, bank account + IFSC, payment requests) and transaction history. */
@Service
@Transactional(noRollbackFor = PinException.class)
public class PaymentService {
    private static final LocalDateTime MIN = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime MAX = LocalDateTime.of(2999, 1, 1, 0, 0);

    private final UserRepository users;
    private final BankAccountRepository accounts;
    private final TransactionRepository txs;
    private final PaymentRequestRepository requests;
    private final UpiRepository upis;
    private final PinService pins;
    private final LedgerService ledger;
    private final UpiLookup lookup;
    private final AuditService audit;
    private final NotificationService notify;
    private final BigDecimal perTxLimit;
    private final BigDecimal dailyLimit;

    public PaymentService(UserRepository users, BankAccountRepository accounts, TransactionRepository txs,
                          PaymentRequestRepository requests, UpiRepository upis, PinService pins, LedgerService ledger,
                          UpiLookup lookup, AuditService audit, NotificationService notify,
                          @Value("${app.limits.per-transaction}") BigDecimal perTxLimit,
                          @Value("${app.limits.daily}") BigDecimal dailyLimit) {
        this.users = users; this.accounts = accounts; this.txs = txs; this.requests = requests; this.upis = upis;
        this.pins = pins; this.ledger = ledger; this.lookup = lookup; this.audit = audit; this.notify = notify;
        this.perTxLimit = perTxLimit; this.dailyLimit = dailyLimit;
    }

    // ------------------------------------------------------------ pay
    public TransactionDto pay(AuthUser me, PayRequest r) {
        BigDecimal amount = r.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(perTxLimit) > 0) throw new BankException("Per-transaction limit is " + Mappers.inr(perTxLimit));

        // 1) payment request (optional)
        PaymentRequest pr = null;
        if (r.requestId() != null) {
            pr = requests.findById(r.requestId()).orElseThrow(() -> new BankException("Request not found", HttpStatus.NOT_FOUND));
            if (!pr.getPayer().getId().equals(me.id())) throw new BankException("This request is not addressed to you", HttpStatus.FORBIDDEN);
            if (pr.getStatus() != RequestStatus.PENDING) throw new BankException("This request is no longer pending");
            if (pr.getAmount().compareTo(amount) != 0) throw new BankException("Amount must match the requested amount");
        }

        // 2) resolve the receiving account (not locked yet)
        BankAccount target;
        String toVpa = null;
        TxChannel channel;
        if (pr != null) {
            UpiId u = upis.findByUserIdAndPrimaryVpaTrue(pr.getRequester().getId())
                    .orElseThrow(() -> new BankException("The requester has no linked bank account", HttpStatus.NOT_FOUND));
            target = u.getAccount();
            toVpa = u.getVpa();
            channel = TxChannel.REQUEST;
        } else if (r.vpa() != null && !r.vpa().isBlank()) {
            UpiId u = lookup.find(r.vpa());
            target = u.getAccount();
            toVpa = u.getVpa();
            channel = "QR".equalsIgnoreCase(r.channel()) ? TxChannel.QR : TxChannel.UPI_ID;
        } else if (r.accountNumber() != null && !r.accountNumber().isBlank() && r.ifsc() != null && !r.ifsc().isBlank()) {
            target = accounts.findByAccountNumberAndBranch_Ifsc(r.accountNumber().trim(), r.ifsc().trim().toUpperCase())
                    .orElseThrow(() -> new BankException("No account found for this account number and IFSC", HttpStatus.NOT_FOUND));
            channel = TxChannel.BANK_ACCOUNT;
        } else {
            throw new BankException("Enter a UPI ID or bank account details");
        }
        if (target.getId().equals(r.fromAccountId())) throw new BankException("You cannot pay the same account");

        // 3) lock both rows in id order (prevents deadlocks and double spending)
        long lo = Math.min(r.fromAccountId(), target.getId()), hi = Math.max(r.fromAccountId(), target.getId());
        BankAccount first = accounts.findByIdForUpdate(lo).orElseThrow(() -> new BankException("Account not found", HttpStatus.NOT_FOUND));
        BankAccount second = accounts.findByIdForUpdate(hi).orElseThrow(() -> new BankException("Account not found", HttpStatus.NOT_FOUND));
        BankAccount from = first.getId().equals(r.fromAccountId()) ? first : second;
        BankAccount to = from == first ? second : first;

        if (!from.getOwner().getId().equals(me.id())) throw new BankException("This is not your account", HttpStatus.FORBIDDEN);
        if (from.getStatus() != AccountStatus.ACTIVE) throw new BankException("Your " + from.getBank().getShortName() + " account is " + from.getStatus().name().toLowerCase() + ". Please contact the bank.");
        if (to.getStatus() != AccountStatus.ACTIVE) throw new BankException("The receiving account cannot accept payments right now");

        // 4) PIN (attempt counters are committed even when this throws)
        pins.verify(from, r.pin(), me.id(), me.email());

        // 5) limits & balance
        BigDecimal today = txs.sumSince(me.id(), TxType.TRANSFER, LocalDate.now().atStartOfDay());
        if (today.add(amount).compareTo(dailyLimit) > 0)
            throw new BankException("Daily limit of " + Mappers.inr(dailyLimit) + " exceeded. Used today: " + Mappers.inr(today));
        if (from.getBalance().compareTo(amount) < 0) throw new BankException("Insufficient balance in your " + from.getBank().getShortName() + " account");

        // 6) move money + ledger
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        User payer = users.findById(me.id()).orElseThrow();
        String fromVpa = upis.findByUserIdAndPrimaryVpaTrue(me.id()).map(UpiId::getVpa).orElse(null);
        BankTransaction t = ledger.record(TxType.TRANSFER, channel, from, to, amount, fromVpa, toVpa, r.remark(), payer);

        if (pr != null) {
            pr.setStatus(RequestStatus.PAID);
            pr.setTransaction(t);
            pr.setResolvedAt(LocalDateTime.now());
        }
        notify.push(to.getOwner(), "Money received", Mappers.inr(amount) + " received from " + payer.getFullName() + " in your " + to.getBank().getShortName() + " account", "CREDIT");
        audit.log(me.id(), me.email(), "PAYMENT", "TRANSACTION", t.getReference(), Mappers.inr(amount) + " via " + channel + " to " + Mappers.maskAcc(to.getAccountNumber()), true);
        return Mappers.tx(t, Set.of(from.getId()), from.getId());
    }

    // ------------------------------------------------------------ history
    @Transactional(readOnly = true)
    public PageDto<TransactionDto> history(AuthUser me, Long accountId, LocalDate from, LocalDate to, int page, int size) {
        List<BankAccount> mine = accounts.findByOwnerId(me.id());
        Set<Long> ids = mine.stream().map(BankAccount::getId).collect(Collectors.toSet());
        Long focus = null;
        Set<Long> query = ids;
        if (accountId != null) {
            if (!ids.contains(accountId)) throw new BankException("This is not your account", HttpStatus.FORBIDDEN);
            focus = accountId;
            query = Set.of(accountId);
        }
        if (ids.isEmpty()) return new PageDto<>(List.of(), 0, 0, 0);
        LocalDateTime f = from == null ? MIN : from.atStartOfDay();
        LocalDateTime t = to == null ? MAX : to.plusDays(1).atStartOfDay();
        Page<BankTransaction> p = txs.findForAccounts(query, f, t, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        final Long fo = focus;
        return new PageDto<>(p.getContent().stream().map(x -> Mappers.tx(x, ids, fo)).toList(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    // ------------------------------------------------------------ payment requests
    private PaymentRequestDto dto(PaymentRequest r, Long viewer) {
        String reqVpa = upis.findByUserIdAndPrimaryVpaTrue(r.getRequester().getId()).map(UpiId::getVpa).orElse(null);
        String payVpa = upis.findByUserIdAndPrimaryVpaTrue(r.getPayer().getId()).map(UpiId::getVpa).orElse(null);
        return new PaymentRequestDto(r.getId(), r.getRequester().getFullName(), reqVpa, r.getPayer().getFullName(), payVpa,
                r.getAmount(), r.getNote(), r.getStatus().name(), r.getRequester().getId().equals(viewer) ? "OUTGOING" : "INCOMING",
                r.getCreatedAt(), r.getResolvedAt());
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestDto> requests(AuthUser me) {
        return requests.findByPayerIdOrRequesterIdOrderByCreatedAtDesc(me.id(), me.id()).stream().map(r -> dto(r, me.id())).toList();
    }

    public PaymentRequestDto createRequest(AuthUser me, CreateRequestRequest r) {
        if (upis.findByUserIdAndPrimaryVpaTrue(me.id()).isEmpty())
            throw new BankException("Link a bank account first so you can receive money");
        UpiId target = lookup.find(r.payee());
        if (target.getUser().getId().equals(me.id())) throw new BankException("You cannot request money from yourself");
        PaymentRequest pr = new PaymentRequest();
        User requester = users.findById(me.id()).orElseThrow();
        pr.setRequester(requester);
        pr.setPayer(target.getUser());
        pr.setAmount(r.amount().setScale(2, RoundingMode.HALF_UP));
        pr.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        requests.save(pr);
        notify.push(target.getUser(), "Payment request", requester.getFullName() + " requested " + Mappers.inr(pr.getAmount()) + (pr.getNote() == null ? "" : " for " + pr.getNote()), "REQUEST");
        audit.log(me.id(), me.email(), "REQUEST_CREATED", "PAYMENT_REQUEST", String.valueOf(pr.getId()), Mappers.inr(pr.getAmount()) + " from " + target.getVpa(), true);
        return dto(pr, me.id());
    }

    private PaymentRequest pending(Long id) {
        PaymentRequest pr = requests.findById(id).orElseThrow(() -> new BankException("Request not found", HttpStatus.NOT_FOUND));
        if (pr.getStatus() != RequestStatus.PENDING) throw new BankException("This request is no longer pending");
        return pr;
    }

    public MessageDto decline(AuthUser me, Long id) {
        PaymentRequest pr = pending(id);
        if (!pr.getPayer().getId().equals(me.id())) throw new BankException("Not allowed", HttpStatus.FORBIDDEN);
        pr.setStatus(RequestStatus.DECLINED);
        pr.setResolvedAt(LocalDateTime.now());
        notify.push(pr.getRequester(), "Request declined", pr.getPayer().getFullName() + " declined your request for " + Mappers.inr(pr.getAmount()), "INFO");
        return new MessageDto("Request declined");
    }

    public MessageDto cancel(AuthUser me, Long id) {
        PaymentRequest pr = pending(id);
        if (!pr.getRequester().getId().equals(me.id())) throw new BankException("Not allowed", HttpStatus.FORBIDDEN);
        pr.setStatus(RequestStatus.CANCELLED);
        pr.setResolvedAt(LocalDateTime.now());
        return new MessageDto("Request cancelled");
    }
}
