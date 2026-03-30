package com.dionialves.AsteraComm.asterisk.endpointidip;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ps_endpoint_id_ips")
public class PsEndpointIdIp {

    @Id
    private String id;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String match;
}
