package com.electricitymonitor.service;

import com.electricitymonitor.model.Meter;
import com.electricitymonitor.model.Reading;
import com.electricitymonitor.model.ReadingSource;
import com.electricitymonitor.repository.MeterRepository;
import com.electricitymonitor.repository.ReadingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Every reading goes through here: save it, move the meter forward,
 * update the hour/day/month/year totals, then run the alert rules.
 * Writes are serialized with a lock so the auto simulator and the
 * simulate button can't create duplicate summary rows.
 */
@Service
public class ReadingService {

    private final MeterRepository meterRepository;
    private final ReadingRepository readingRepository;
    private final UsageService usageService;
    private final AlertService alertService;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final Object lock = new Object();

    public ReadingService(MeterRepository meterRepository,
                          ReadingRepository readingRepository,
                          UsageService usageService,
                          AlertService alertService,
                          TransactionTemplate tx,
                          Clock clock) {
        this.meterRepository = meterRepository;
        this.readingRepository = readingRepository;
        this.usageService = usageService;
        this.alertService = alertService;
        this.tx = tx;
        this.clock = clock;
    }

    /** Add one reading in its own transaction, with alerts. */
    public Reading add(Long meterId, double powerWatts, int durationSeconds,
                       LocalDateTime recordedAt, ReadingSource source) {
        synchronized (lock) {
            return tx.execute(status ->
                    addNow(meterId, powerWatts, durationSeconds, recordedAt, source, true));
        }
    }

    /** Run several steps as one locked transaction (used for history and reset). */
    public <T> T runLocked(Supplier<T> work) {
        synchronized (lock) {
            return tx.execute(status -> work.get());
        }
    }

    /** Do the work. Call this only from inside add() or runLocked(). */
    public Reading addNow(Long meterId, double powerWatts, int durationSeconds,
                          LocalDateTime recordedAt, ReadingSource source, boolean checkAlerts) {
        Meter meter = meterRepository.findById(meterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meter not found"));

        LocalDateTime time = recordedAt;
        if (time == null) {
            time = LocalDateTime.now(clock);
        }

        double usedKwh = powerWatts * durationSeconds / 3_600_000.0;
        double cumulative = meter.getCurrentReading() + usedKwh;

        Reading reading = new Reading();
        reading.setMeterId(meterId);
        reading.setRecordedAt(time);
        reading.setPowerWatts(powerWatts);
        reading.setDurationSeconds(durationSeconds);
        reading.setUsedKwh(usedKwh);
        reading.setCumulativeKwh(cumulative);
        reading.setSource(source);
        readingRepository.save(reading);

        meter.setCurrentReading(cumulative);
        meterRepository.save(meter);

        usageService.addUsage(meterId, time, usedKwh, powerWatts);

        if (checkAlerts) {
            alertService.check(meter, reading);
        }
        return reading;
    }
}
