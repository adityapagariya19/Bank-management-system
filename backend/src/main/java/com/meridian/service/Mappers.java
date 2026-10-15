package com.meridian.service;

import com.meridian.dto.Dtos.*;
import com.meridian.model.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

public final class Mappers {
    private Mappers() {}

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"));

    public static String inr(BigDecimal v) { return INR.format(v); }

    public static String maskAcc(String n) {
        return n == null || n.length() < 4 ? n : "XXXX" + n.substring(n.length() - 4);
    }

    public static AccountDto account(BankAccount a) {
        boolean locked = a.getPinLockedUntil() != null && a.getPinLockedUntil().isAfter(LocalDateTime.now());
        Bank b = a.getBank();
        return new AccountDto(a.getId(), a.getAccountNumber(), b.getId(), b.getName(), b.getShortName(), b.getColor(),
                a.getBranch().getName(), a.getBranch().getIfsc(), a.getType().name(), a.getBalance(), a.getStatus().name(),
                a.isPrimaryAccount(), locked, a.getOpenedAt(), a.getOwner().getFullName());
    }

    public static AdminAccountDto adminAccount(BankAccount a) {
        return new AdminAccountDto(a.getId(), a.getAccountNumber(), a.getOwner().getFullName(), a.getOwner().getEmail(),
                a.getBank().getName(), a.getBranch().getIfsc(), a.getType().name(), a.getBalance(), a.getStatus().name(), a.getOpenedAt());
    }

    /** mine = account ids belonging to the viewer; focus = account whose statement is being viewed (nullable). */
    public static TransactionDto tx(BankTransaction t, Set<Long> mine, Long focus) {
        BankAccount f = t.getFromAccount(), to = t.getToAccount();
        boolean fm = f != null && mine.contains(f.getId());
        boolean tm = to != null && mine.contains(to.getId());
        String dir;
        BigDecimal after = null;
        if (focus != null) {
            boolean fromFocus = f != null && f.getId().equals(focus);
            dir = fromFocus ? "DEBIT" : "CREDIT";
            after = fromFocus ? t.getFromBalanceAfter() : t.getToBalanceAfter();
        } else if (fm && tm) dir = "SELF";
        else if (tm) dir = "CREDIT";
        else if (fm) dir = "DEBIT";
        else dir = "NONE";
        return new TransactionDto(t.getId(), t.getReference(), t.getType().name(), t.getChannel().name(), t.getStatus(),
                t.getAmount(), t.getRemark(), dir,
                f == null ? null : f.getOwner().getFullName(), f == null ? null : f.getBank().getName(), f == null ? null : maskAcc(f.getAccountNumber()),
                to == null ? null : to.getOwner().getFullName(), to == null ? null : to.getBank().getName(), to == null ? null : maskAcc(to.getAccountNumber()),
                t.getFromVpa(), t.getToVpa(), after, t.getPerformedBy() == null ? null : t.getPerformedBy().getFullName(), t.getCreatedAt());
    }

    public static KycDto kyc(KycProfile k) {
        User u = k.getUser();
        return new KycDto(k.getId(), u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), k.getDateOfBirth(), k.getAddress(),
                k.getCity(), k.getState(), k.getPincode(), k.getIdType(), k.getIdNumberMasked(), k.getStatus().name(), k.getReviewNote(),
                k.getReviewedBy() == null ? null : k.getReviewedBy().getFullName(), k.getSubmittedAt(), k.getReviewedAt());
    }

    public static NotificationDto notification(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.isSeen(), n.getCreatedAt());
    }

    public static AuditDto audit(AuditLog a) {
        return new AuditDto(a.getId(), a.getActorEmail(), a.getAction(), a.getEntity(), a.getEntityId(), a.getDetails(),
                a.getIpAddress(), a.isSuccess(), a.getCreatedAt());
    }
}
