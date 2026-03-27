package com.dionialves.AsteraComm.asterisk.extension;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "extensions")
public class Extension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String context;

    @Column(name = "exten", nullable = false, length = 40)
    private String exten;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false, length = 20)
    private String app;

    @Column(nullable = true, length = 128)
    private String appdata;
}
