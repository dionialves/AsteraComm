package com.dionialves.AsteraComm.trunk;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asteracomm_trunk_registration_status")
public class TrunkRegistrationStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trunk_name", nullable = false)
    private String trunkName;

    @Column(nullable = false)
    private boolean registered;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    public TrunkRegistrationStatus(String trunkName, boolean registered, LocalDateTime checkedAt) {
        this.trunkName = trunkName;
        this.registered = registered;
        this.checkedAt = checkedAt;
    }
}
