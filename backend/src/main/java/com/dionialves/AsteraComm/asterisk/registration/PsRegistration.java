package com.dionialves.AsteraComm.asterisk.registration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "ps_registrations")
public class PsRegistration {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "server_uri", nullable = false)
    private String serverUri;

    @Column(name = "client_uri", nullable = false)
    private String clientUri;

    @Column(name = "outbound_auth")
    private String outboundAuth;

    @Column(name = "retry_interval")
    private String retryInterval;
}
