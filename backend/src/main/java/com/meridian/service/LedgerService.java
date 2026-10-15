package com.meridian.service;

import com.meridian.model.*;
import com.meridian.repo.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

/** Writes immutable ledger rows. Call AFTER the account balances have been updated so balance-after is exact. */
@Service
public class LedgerService {
    private final TransactionRepository txs;
    private final SecureRandom rnd = new SecureRandom();

    public LedgerService(TransactionRepository txs) {
        this.txs = txs;
    }

    @Transactional
    public BankTransaction record(TxType type, TxChannel channel, BankAccount from, BankAccount to, BigDecimal amount,
                                  String fromVpa, String toVpa, String remark, User performedBy) {
        BankTransaction t = new BankTransaction();
        String ref;
        do {
            ref = String.valueOf(100_000_000_000L + Math.floorMod(rnd.nextLong(), 900_000_000_000L));
        } while (txs.existsByReference(ref));
        t.setReference(ref);
        t.setType(type);
        t.setChannel(channel);
        t.setAmount(amount);
        t.setFromAccount(from);
        t.setToAccount(to);
        t.setFromVpa(fromVpa);
        t.setToVpa(toVpa);
        t.setRemark(remark == null || remark.isBlank() ? null : remark.trim());
        t.setPerformedBy(performedBy);
        t.setFromBalanceAfter(from == null ? null : from.getBalance());
        t.setToBalanceAfter(to == null ? null : to.getBalance());
        return txs.save(t);
    }
}
