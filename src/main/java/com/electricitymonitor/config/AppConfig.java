package com.electricitymonitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    // One clock for the whole app, so every "now" uses the same time zone.
    @Bean
    public Clock clock(@Value("${app.zone:Asia/Bangkok}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
