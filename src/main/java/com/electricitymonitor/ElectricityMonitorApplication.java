package com.electricitymonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElectricityMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElectricityMonitorApplication.class, args);
    }
}
