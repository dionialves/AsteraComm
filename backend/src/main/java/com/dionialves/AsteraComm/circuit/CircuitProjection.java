package com.dionialves.AsteraComm.circuit;

public interface CircuitProjection {
    Long getId();
    String getNumber();
    String getPassword();
    String getTrunkName();
    String getCustomerName();
    String getPlanName();
    String getIp();
    String getRtt();
    boolean isOnline();
    boolean isActive();
}
