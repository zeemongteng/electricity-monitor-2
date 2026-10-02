package com.electricitymonitor.controller;

import com.electricitymonitor.dto.Dtos.AddReadingRequest;
import com.electricitymonitor.model.Reading;
import com.electricitymonitor.model.ReadingSource;
import com.electricitymonitor.repository.ReadingRepository;
import com.electricitymonitor.service.ReadingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/readings")
public class ReadingController {

    private final ReadingService readingService;
    private final ReadingRepository readingRepository;

    public ReadingController(ReadingService readingService, ReadingRepository readingRepository) {
        this.readingService = readingService;
        this.readingRepository = readingRepository;
    }

    /** The 50 newest readings of a meter. */
    @GetMapping
    public List<Reading> latest(@RequestParam Long meterId) {
        return readingRepository.findTop50ByMeterIdOrderByRecordedAtDesc(meterId);
    }

    /** Where a real device would post its readings later. */
    @PostMapping
    public Reading add(@Valid @RequestBody AddReadingRequest request) {
        return ReadingEndpoints.add(readingService, request, ReadingSource.DEVICE);
    }
}
