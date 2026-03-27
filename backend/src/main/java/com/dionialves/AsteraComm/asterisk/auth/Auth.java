package com.dionialves.AsteraComm.asterisk.auth;

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
@Table(name = "ps_auths")
public class Auth {

    @Id
    @Column(name = "id")
    private String id;

    @Column(nullable = false, name = "auth_type")
    private String authType;

    private String username;
    private String password;

}
