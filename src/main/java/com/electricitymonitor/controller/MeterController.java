package com.electricitymonitor.controller;

import com.electricitymonitor.dto.Dtos.MeterRequest;
import com.electricitymonitor.dto.Dtos.MeterSettings;
import com.electricitymonitor.dto.Dtos.Overview;
import com.electricitymonitor.model.Meter;
import com.electricitymonitor.repository.MeterRepository;
import com.electricitymonitor.service.UsageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/meters")
public class MeterController {

    private final MeterRepository meterRepository;
    private final UsageService usageService;
    private final Clock clock;

    public MeterController(MeterRepository meterRepository, UsageService usageService, Clock clock) {
        this.meterRepository = meterRepository;
        this.usageService = usageService;
        this.clock = clock;
    }

    @GetMapping
    public List<Meter> list() {
        return meterRepository.findAll(Sort.by("id"));
    }

    @PostMapping
    public Meter create(@Valid @RequestBody MeterRequest request) {
        double initial = 0;
        if (request.initialReading() != null) {
            initial = request.initialReading();
        }
        Meter meter = new Meter();
        meter.setName(request.name());
        meter.setLocation(request.location());
        meter.setInitialReading(initial);
        meter.setCurrentReading(initial);
        meter.setCreatedAt(LocalDateTime.now(clock));
        return meterRepository.save(meter);
    }

    @PutMapping("/{id}")
    public Meter update(@PathVariable Long id, @Valid @RequestBody MeterSettings settings) {
        Meter meter = find(id);
        meter.setName(settings.name());
        meter.setAlertMultiplier(settings.alertMultiplier());
        meter.setAlertsEnabled(settings.alertsEnabled());
        meter.setDailyBudgetKwh(settings.dailyBudgetKwh());
        meter.setRatePerKwh(settings.ratePerKwh());
        return meterRepository.save(meter);
    }

    @GetMapping("/{id}/overview")
    public Overview overview(@PathVariable Long id) {
        return usageService.overview(find(id));
    }

    private Meter find(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meter not found"));
    }
}
