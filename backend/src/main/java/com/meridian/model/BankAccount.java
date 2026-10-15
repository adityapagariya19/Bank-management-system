package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
@Getter @Setter @NoArgsConstructor
public class BankAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20) private String accountNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "bank_id") private Bank bank;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "branch_id") private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id") private User owner;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AccountType type = AccountType.SAVINGS;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal balance = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AccountStatus status = AccountStatus.ACTIVE;
    @Column(nullable = false) private String pinHash;
    @Column(nullable = false) private int pinAttempts = 0;
    private LocalDateTime pinLockedUntil;
    @Column(nullable = false) private boolean primaryAccount = false;
    @Column(nullable = false, updatable = false) private LocalDateTime openedAt = LocalDateTime.now();
}
