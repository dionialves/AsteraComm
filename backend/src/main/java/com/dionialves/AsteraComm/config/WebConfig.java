package com.dionialves.AsteraComm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.dionialves.AsteraComm.infra.interceptor.CurrentPathInterceptor;
import com.dionialves.AsteraComm.infra.interceptor.RequestTimingInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    RequestTimingInterceptor timingInterceptor;

    @Autowired
    CurrentPathInterceptor currentPathInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(timingInterceptor);
        registry.addInterceptor(currentPathInterceptor);
    }
}
