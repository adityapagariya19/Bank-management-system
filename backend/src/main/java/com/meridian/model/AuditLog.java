package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorId;
    @Column(length = 150) private String actorEmail;
    @Column(nullable = false, length = 60) private String action;
    @Column(length = 40) private String entity;
    @Column(length = 40) private String entityId;
    @Column(length = 500) private String details;
    @Column(length = 45) private String ipAddress;
    @Column(nullable = false) private boolean success = true;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
