package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "banks")
@Getter @Setter @NoArgsConstructor
public class Bank {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 30) private String shortName;
    @Column(nullable = false, length = 10) private String color;
    @Column(length = 80) private String headquarters;
    @Column(nullable = false) private boolean active = true;
}
