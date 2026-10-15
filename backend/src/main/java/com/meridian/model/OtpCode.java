package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
@Getter @Setter @NoArgsConstructor
public class OtpCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OtpPurpose purpose;
    @Column(nullable = false) private String codeHash;
    @Column(nullable = false) private LocalDateTime expiresAt;
    @Column(nullable = false) private int attempts = 0;
    @Column(nullable = false) private boolean consumed = false;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
