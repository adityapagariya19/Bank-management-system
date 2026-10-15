package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries")
@Getter @Setter @NoArgsConstructor
public class Beneficiary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id") private User owner;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 60) private String vpa;
    @Column(length = 60) private String nickname;
}
