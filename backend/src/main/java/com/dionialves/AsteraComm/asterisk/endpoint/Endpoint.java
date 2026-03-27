package com.dionialves.AsteraComm.asterisk.endpoint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.dionialves.AsteraComm.asterisk.auth.Auth;
import com.dionialves.AsteraComm.asterisk.aors.Aors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ps_endpoints")
public class Endpoint {

    @Id
    @Column(name = "id")
    private String id;

    private String transport;

    @OneToOne
    @JoinColumn(name = "aors", referencedColumnName = "id")
    private Aors aors;

    @OneToOne
    @JoinColumn(name = "auth", referencedColumnName = "id")
    private Auth auth;

    private String context;
    private String disallow;
    private String allow;
    private String direct_media;
    private String force_rport;

    @Column(name = "rtp_symmetric")
    private String rtpSymmetric;

    @Column(name = "rewrite_contact")
    private String rewriteContact;

    private String callerid;

    @Column(name = "mailboxes")
    private String mailBoxes;

    @Column(name = "outbound_auth")
    private String outboundAuth;

}
