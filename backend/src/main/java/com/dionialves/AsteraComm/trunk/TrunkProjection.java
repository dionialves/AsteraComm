package com.dionialves.AsteraComm.trunk;

public interface TrunkProjection {
    Long getId();
    String getName();
    String getHost();
    String getUsername();
    String getAuthType();
    boolean isRegistered();
}
