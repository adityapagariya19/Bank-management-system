package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_requests")
@Getter @Setter @NoArgsConstructor
public class PaymentRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requester_id") private User requester;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payer_id") private User payer;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(length = 120) private String note;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RequestStatus status = RequestStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transaction_id") private BankTransaction transaction;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;
}
