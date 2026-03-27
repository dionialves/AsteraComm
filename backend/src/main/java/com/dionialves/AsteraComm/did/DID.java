package com.dionialves.AsteraComm.did;

import com.dionialves.AsteraComm.circuit.Circuit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "asteracomm_dids")
public class DID {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "circuit_id")
    @JsonIgnore
    private Circuit circuit;

    @JsonProperty("circuitId")
    public Long getCircuitId() {
        return circuit != null ? circuit.getId() : null;
    }

    @JsonProperty("circuitNumber")
    public String getCircuitNumber() {
        return circuit != null ? circuit.getNumber() : null;
    }
}
