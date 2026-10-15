package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "upi_ids")
@Getter @Setter @NoArgsConstructor
public class UpiId {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60) private String vpa;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id") private BankAccount account;
    @Column(nullable = false) private boolean primaryVpa = false;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
