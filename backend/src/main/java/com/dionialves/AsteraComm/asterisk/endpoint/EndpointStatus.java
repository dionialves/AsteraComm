package com.dionialves.AsteraComm.asterisk.endpoint;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asteracomm_endpoint_status")
public class EndpointStatus {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "endpoint", referencedColumnName = "id", nullable = false)
    private Endpoint endpoint;

    @Column(name = "online", nullable = false)
    private boolean online;

    private String ip;
    private String rtt;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    public EndpointStatus(Endpoint endpoint, boolean online, String ip, String rtt, LocalDateTime checkedAt) {
        this.endpoint = endpoint;
        this.online = online;
        this.rtt = rtt;
        this.checkedAt = checkedAt;
        this.ip = ip;
    }
}
