package com.dionialves.AsteraComm.config;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;

class AppVersionAdviceTest {

    @Test
    void shouldPopulateAppVersionInModel() {
        AppVersionAdvice advice = new AppVersionAdvice();
        advice.setAppVersion("1.2.3");

        ExtendedModelMap model = new ExtendedModelMap();
        model.addAttribute("appVersion", advice.appVersion());

        assertThat(model.get("appVersion")).isEqualTo("1.2.3");
    }

    @Test
    void shouldReturnEmptyStringWhenVersionIsNull() {
        AppVersionAdvice advice = new AppVersionAdvice();
        advice.setAppVersion(null);

        assertThat(advice.appVersion()).isEmpty();
    }

    @Test
    void shouldReturnEmptyStringWhenVersionIsBlank() {
        AppVersionAdvice advice = new AppVersionAdvice();
        advice.setAppVersion("");

        assertThat(advice.appVersion()).isEmpty();
    }
}
