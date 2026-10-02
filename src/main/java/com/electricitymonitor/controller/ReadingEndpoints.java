package com.electricitymonitor.controller;

import com.electricitymonitor.dto.Dtos.AddReadingRequest;
import com.electricitymonitor.model.Reading;
import com.electricitymonitor.model.ReadingSource;
import com.electricitymonitor.service.ReadingService;

/** Shared by the device endpoint and the simulate button. */
final class ReadingEndpoints {

    private ReadingEndpoints() {
    }

    static Reading add(ReadingService readingService, AddReadingRequest request, ReadingSource source) {
        int seconds = 10;
        if (request.durationSeconds() != null) {
            seconds = request.durationSeconds();
        }
        return readingService.add(request.meterId(), request.powerWatts(), seconds, request.recordedAt(), source);
    }
}
