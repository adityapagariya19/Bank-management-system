package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "branches")
@Getter @Setter @NoArgsConstructor
public class Branch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "bank_id") private Bank bank;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, unique = true, length = 11) private String ifsc;
    @Column(nullable = false, length = 80) private String city;
    @Column(nullable = false, length = 80) private String state;
}
