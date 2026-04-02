package com.dionialves.AsteraComm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AppVersionAdvice {

    @Value("${project.version:}")
    private String appVersion;

    void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        return appVersion != null ? appVersion : "";
    }
}
