package com.dionialves.AsteraComm.circuit.dto;

public record CircuitCreateDTO(String password, String trunkName, Long customerId, Long planId, Boolean active) {}
