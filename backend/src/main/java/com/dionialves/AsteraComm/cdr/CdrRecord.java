package com.dionialves.AsteraComm.cdr;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Immutable
@Entity
@Table(name = "cdr")
public class CdrRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uniqueid", nullable = false)
    @JsonProperty("uniqueid")
    private String uniqueId;

    @Column(name = "calldate", nullable = false)
    private LocalDateTime calldate;

    @Column(name = "clid", nullable = false)
    private String clid;

    @Column(name = "src", nullable = false)
    private String src;

    @Column(name = "dst", nullable = false)
    private String dst;

    @Column(name = "dcontext", nullable = false)
    private String dcontext;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "dstchannel", nullable = false)
    private String dstchannel;

    @Column(name = "lastapp", nullable = false)
    private String lastapp;

    @Column(name = "lastdata", nullable = false)
    private String lastdata;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "billsec", nullable = false)
    private Integer billsec;

    @Column(name = "disposition", nullable = false)
    private String disposition;

    @Column(name = "amaflags", nullable = false)
    private Integer amaflags;

    @Column(name = "accountcode")
    private String accountcode;

    @Column(name = "userfield")
    private String userfield;
}
