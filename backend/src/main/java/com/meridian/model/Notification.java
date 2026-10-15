package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, length = 100) private String title;
    @Column(nullable = false, length = 300) private String message;
    @Column(nullable = false, length = 20) private String type = "INFO";
    @Column(nullable = false) private boolean seen = false;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
