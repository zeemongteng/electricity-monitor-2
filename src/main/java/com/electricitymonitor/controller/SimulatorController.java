package com.electricitymonitor.controller;

import com.electricitymonitor.dto.Dtos.AddReadingRequest;
import com.electricitymonitor.dto.Dtos.HistoryRequest;
import com.electricitymonitor.dto.Dtos.SimulatorStatus;
import com.electricitymonitor.model.Reading;
import com.electricitymonitor.model.ReadingSource;
import com.electricitymonitor.service.ReadingService;
import com.electricitymonitor.service.SimulatorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final SimulatorService simulatorService;
    private final ReadingService readingService;

    public SimulatorController(SimulatorService simulatorService, ReadingService readingService) {
        this.simulatorService = simulatorService;
        this.readingService = readingService;
    }

    @GetMapping
    public SimulatorStatus status() {
        return simulatorService.status();
    }

    /** Turn the auto simulator on or off. */
    @PostMapping("/auto")
    public SimulatorStatus auto(@RequestParam boolean enabled) {
        simulatorService.setAutoEnabled(enabled);
        return simulatorService.status();
    }

    /** The simulate button: add one reading with the numbers you choose. */
    @PostMapping("/manual")
    public Reading manual(@Valid @RequestBody AddReadingRequest request) {
        return ReadingEndpoints.add(readingService, request, ReadingSource.MANUAL);
    }

    @PostMapping("/history")
    public Map<String, Integer> history(@Valid @RequestBody HistoryRequest request) {
        int created = simulatorService.generateHistory(request.meterId(), request.days());
        return Map.of("generated", created);
    }

    @PostMapping("/sample")
    public Map<String, Integer> sample(@RequestParam Long meterId) {
        int created = simulatorService.generateSample(meterId);
        return Map.of("generated", created);
    }

    @DeleteMapping("/data")
    public void reset() {
        simulatorService.resetAll();
    }
}
