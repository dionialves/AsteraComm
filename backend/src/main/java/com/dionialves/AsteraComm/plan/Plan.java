package com.dionialves.AsteraComm.plan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "asteracomm_plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "fixed_local", nullable = false, precision = 10, scale = 4)
    private BigDecimal fixedLocal;

    @Column(name = "fixed_long_distance", nullable = false, precision = 10, scale = 4)
    private BigDecimal fixedLongDistance;

    @Column(name = "mobile_local", nullable = false, precision = 10, scale = 4)
    private BigDecimal mobileLocal;

    @Column(name = "mobile_long_distance", nullable = false, precision = 10, scale = 4)
    private BigDecimal mobileLongDistance;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false)
    private PackageType packageType;

    @Column(name = "package_total_minutes")
    private Integer packageTotalMinutes;

    @Column(name = "package_fixed_local")
    private Integer packageFixedLocal;

    @Column(name = "package_fixed_long_distance")
    private Integer packageFixedLongDistance;

    @Column(name = "package_mobile_local")
    private Integer packageMobileLocal;

    @Column(name = "package_mobile_long_distance")
    private Integer packageMobileLongDistance;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
