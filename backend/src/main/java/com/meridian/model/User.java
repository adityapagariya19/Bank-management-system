package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100) private String fullName;
    @Column(nullable = false, unique = true, length = 150) private String email;
    @Column(nullable = false, unique = true, length = 15) private String phone;
    @Column(nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role = Role.CUSTOMER;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private UserStatus status = UserStatus.PENDING_VERIFICATION;
    @Column(nullable = false) private int failedAttempts = 0;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
