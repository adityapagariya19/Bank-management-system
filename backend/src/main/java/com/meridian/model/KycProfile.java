package com.meridian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_profiles")
@Getter @Setter @NoArgsConstructor
public class KycProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", unique = true) private User user;
    private LocalDate dateOfBirth;
    @Column(length = 255) private String address;
    @Column(length = 80) private String city;
    @Column(length = 80) private String state;
    @Column(length = 10) private String pincode;
    @Column(length = 20) private String idType;
    @Column(length = 30) private String idNumberMasked;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private KycStatus status = KycStatus.NOT_SUBMITTED;
    @Column(length = 255) private String reviewNote;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private User reviewedBy;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
