package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.circuit.Circuit;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "asteracomm_calls")
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unique_id", nullable = false, unique = true)
    private String uniqueId;

    @Column(name = "call_date", nullable = false)
    private LocalDateTime callDate;

    @Column(name = "caller_number")
    private String callerNumber;

    @Column(name = "dst", nullable = false)
    private String dst;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "bill_seconds", nullable = false)
    private Integer billSeconds;

    @Column(name = "disposition", nullable = false)
    private String disposition;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false)
    private CallType callType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "circuit_number", referencedColumnName = "number")
    @JsonIgnoreProperties({"customer", "password", "hibernateLazyInitializer", "handler"})
    private Circuit circuit;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_status")
    private CallStatus callStatus;

    @Column(name = "minutes_from_quota", precision = 10, scale = 1)
    private BigDecimal minutesFromQuota;

    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;
}
