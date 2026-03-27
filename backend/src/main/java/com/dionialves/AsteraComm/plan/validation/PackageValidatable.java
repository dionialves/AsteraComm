package com.dionialves.AsteraComm.plan.validation;

import com.dionialves.AsteraComm.plan.PackageType;

public interface PackageValidatable {
    PackageType packageType();
    Integer packageTotalMinutes();
    Integer packageFixedLocal();
    Integer packageFixedLongDistance();
    Integer packageMobileLocal();
    Integer packageMobileLongDistance();
}
