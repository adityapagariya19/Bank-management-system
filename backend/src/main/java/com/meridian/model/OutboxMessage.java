package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_outbox")
@Getter @Setter @NoArgsConstructor
public class OutboxMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10) private String channel;
    @Column(nullable = false, length = 150) private String recipient;
    @Column(length = 150) private String subject;
    @Column(nullable = false, length = 500) private String body;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
