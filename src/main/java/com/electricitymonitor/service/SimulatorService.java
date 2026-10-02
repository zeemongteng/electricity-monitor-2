package com.electricitymonitor.service;

import com.electricitymonitor.dto.Dtos.SimulatorStatus;
import com.electricitymonitor.model.Meter;
import com.electricitymonitor.model.PeriodType;
import com.electricitymonitor.model.ReadingSource;
import com.electricitymonitor.model.UsageSummary;
import com.electricitymonitor.repository.MeterRepository;
import com.electricitymonitor.repository.NotificationRepository;
import com.electricitymonitor.repository.ReadingRepository;
import com.electricitymonitor.repository.UsageSummaryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Fake meter: a daily power curve + noise + occasional spikes. */
@Service
public class SimulatorService {

    // Typical household power (watts) at the start of each hour, 00:00 to 23:00.
    private static final double[] HOURLY_BASE_WATTS = {
            250, 230, 220, 220, 230, 300, 550, 700,
            550, 420, 400, 420, 500, 450, 400, 420,
            500, 750, 1000, 1100, 950, 750, 500, 330
    };

    private static class Spike {
        int ticksLeft;
        double extraWatts;
    }

    private final MeterRepository meterRepository;
    private final ReadingRepository readingRepository;
    private final UsageSummaryRepository usageSummaryRepository;
    private final NotificationRepository notificationRepository;
    private final ReadingService readingService;
    private final Clock clock;
    private final long intervalMs;

    private final Random random = new Random();
    private final Map<Long, Spike> spikes = new HashMap<>();
    private volatile boolean autoEnabled;

    public SimulatorService(MeterRepository meterRepository,
                            ReadingRepository readingRepository,
                            UsageSummaryRepository usageSummaryRepository,
                            NotificationRepository notificationRepository,
                            ReadingService readingService,
                            Clock clock,
                            @Value("${simulator.enabled:true}") boolean enabled,
                            @Value("${simulator.interval-ms:10000}") long intervalMs) {
        this.meterRepository = meterRepository;
        this.readingRepository = readingRepository;
        this.usageSummaryRepository = usageSummaryRepository;
        this.notificationRepository = notificationRepository;
        this.readingService = readingService;
        this.clock = clock;
        this.autoEnabled = enabled;
        this.intervalMs = intervalMs;
    }

    public SimulatorStatus status() {
        return new SimulatorStatus(autoEnabled, intervalMs);
    }

    public void setAutoEnabled(boolean enabled) {
        this.autoEnabled = enabled;
    }

    // ---------- auto simulator ----------

    @Scheduled(fixedDelayString = "${simulator.interval-ms:10000}")
    public void tick() {
        if (!autoEnabled) {
            return;
        }
        int seconds = (int) Math.max(1, intervalMs / 1000);
        LocalDateTime now = LocalDateTime.now(clock);
        for (Meter meter : meterRepository.findAll()) {
            double watts = nextAutoWatts(meter, now);
            readingService.add(meter.getId(), watts, seconds, now, ReadingSource.AUTO);
        }
    }

    private double nextAutoWatts(Meter meter, LocalDateTime now) {
        // Give each meter a slightly different size so they don't look identical.
        double scale = 0.7 + 0.15 * (meter.getId() % 5);
        double watts = baseWatts(now) * scale * (1 + random.nextGaussian() * 0.05);

        Spike spike = spikes.get(meter.getId());
        if (spike == null) {
            spike = new Spike();
            spikes.put(meter.getId(), spike);
        }
        if (spike.ticksLeft > 0) {
            spike.ticksLeft--;
            watts += spike.extraWatts;
        } else if (random.nextDouble() < 0.02) {
            // Something big (an air conditioner, a heater) switches on for a few ticks.
            spike.ticksLeft = 3 + random.nextInt(4);
            spike.extraWatts = 1500 + random.nextDouble() * 1000;
            watts += spike.extraWatts;
        }
        return Math.max(50, watts);
    }

    /** Smooth curve: blend between this hour's and next hour's base power by the minute. */
    private double baseWatts(LocalDateTime t) {
        int hour = t.getHour();
        double fraction = t.getMinute() / 60.0;
        double a = HOURLY_BASE_WATTS[hour];
        double b = HOURLY_BASE_WATTS[(hour + 1) % 24];
        return a + (b - a) * fraction;
    }

    // ---------- history ----------

    /**
     * Fill the time before the oldest existing data with one reading per hour.
     * Call it again to go further back. Returns how many readings were created.
     */
    public int generateHistory(Long meterId, int days) {
        return readingService.runLocked(() -> {
            UsageSummary earliest = usageSummaryRepository
                    .findFirstByMeterIdAndPeriodTypeOrderByPeriodStartAsc(meterId, PeriodType.HOUR)
                    .orElse(null);
            LocalDateTime end;
            if (earliest != null) {
                end = earliest.getPeriodStart();
            } else {
                end = PeriodType.HOUR.startOf(LocalDateTime.now(clock));
            }
            LocalDateTime start = end.minusDays(days);
            int totalHours = days * 24;

            double dayFactor = 1.0;
            int created = 0;
            for (int i = 0; i < totalHours; i++) {
                LocalDateTime hourStart = start.plusHours(i);
                if (i == 0 || hourStart.getHour() == 0) {
                    // Each day is a bit busier or quieter than the last; weekends use more.
                    dayFactor = 0.85 + random.nextDouble() * 0.3;
                    DayOfWeek dow = hourStart.getDayOfWeek();
                    if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                        dayFactor *= 1.1;
                    }
                }
                LocalDateTime middle = hourStart.plusMinutes(30);
                double watts = baseWatts(middle) * dayFactor * (1 + random.nextGaussian() * 0.08);
                watts = Math.max(50, watts);
                readingService.addNow(meterId, watts, 3600, middle, ReadingSource.HISTORY, false);
                created++;
            }
            return created;
        });
    }

    // ---------- reset ----------

    /** Delete all readings, totals and notifications; put every meter back to its initial reading. */
    public void resetAll() {
        readingService.runLocked(() -> {
            readingRepository.deleteAllInBatch();
            usageSummaryRepository.deleteAllInBatch();
            notificationRepository.deleteAllInBatch();
            List<Meter> meters = meterRepository.findAll();
            for (Meter meter : meters) {
                meter.setCurrentReading(meter.getInitialReading());
                meterRepository.save(meter);
            }
            return null;
        });
    }
}
