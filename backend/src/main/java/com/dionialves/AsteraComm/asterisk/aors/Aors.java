package com.dionialves.AsteraComm.asterisk.aors;

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
@Table(name = "ps_aors")
public class Aors {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "default_expiration")
    private String defaultExpiration;

    private String contact;

    @Column(name = "max_contacts")
    private String maxContacts;

    @Column(name = "remove_existing")
    private String removeExisting;

    @Column(name = "qualify_frequency")
    private String qualifyFrequency;
}
