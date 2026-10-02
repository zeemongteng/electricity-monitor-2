package com.electricitymonitor.service;

import com.electricitymonitor.repository.ReadingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** At 10 s per reading the raw table grows fast, so old raw rows are deleted. Totals stay. */
@Component
public class RetentionJob {

    private final ReadingRepository readingRepository;
    private final Clock clock;
    private final int retentionDays;

    public RetentionJob(ReadingRepository readingRepository,
                        Clock clock,
                        @Value("${retention.days:7}") int retentionDays) {
        this.readingRepository = readingRepository;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "${app.zone:Asia/Bangkok}")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
        readingRepository.deleteOlderThan(cutoff);
    }
}
