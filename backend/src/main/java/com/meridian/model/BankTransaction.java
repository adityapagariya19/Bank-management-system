package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor
public class BankTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20) private String reference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TxType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TxChannel channel;
    @Column(nullable = false, length = 20) private String status = "SUCCESS";
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(length = 120) private String remark;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "from_account_id") private BankAccount fromAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "to_account_id") private BankAccount toAccount;
    @Column(length = 60) private String fromVpa;
    @Column(length = 60) private String toVpa;
    @Column(precision = 15, scale = 2) private BigDecimal fromBalanceAfter;
    @Column(precision = 15, scale = 2) private BigDecimal toBalanceAfter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "performed_by") private User performedBy;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
