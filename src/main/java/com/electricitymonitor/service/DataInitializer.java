package com.electricitymonitor.service;

import com.electricitymonitor.model.Meter;
import com.electricitymonitor.repository.MeterRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/** Creates one meter on first start so the dashboard has something to show. */
@Component
public class DataInitializer implements ApplicationRunner {

    private final MeterRepository meterRepository;
    private final Clock clock;

    public DataInitializer(MeterRepository meterRepository, Clock clock) {
        this.meterRepository = meterRepository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (meterRepository.count() > 0) {
            return;
        }
        Meter meter = new Meter();
        meter.setName("Main meter");
        meter.setLocation("Home");
        meter.setInitialReading(1520.4);
        meter.setCurrentReading(1520.4);
        meter.setCreatedAt(LocalDateTime.now(clock));
        meterRepository.save(meter);
    }
}
