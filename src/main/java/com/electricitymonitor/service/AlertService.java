package com.electricitymonitor.service;

import com.electricitymonitor.model.Meter;
import com.electricitymonitor.model.Notification;
import com.electricitymonitor.model.NotificationKind;
import com.electricitymonitor.model.PeriodType;
import com.electricitymonitor.model.Reading;
import com.electricitymonitor.model.UsageSummary;
import com.electricitymonitor.repository.NotificationRepository;
import com.electricitymonitor.repository.ReadingRepository;
import com.electricitymonitor.repository.UsageSummaryRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Three alert rules, all based on "more than N times the average" where N is the
 * meter's alertMultiplier (the set point):
 *   SPIKE        - this reading's power vs. the average of the last 30 readings
 *   HOURLY_HIGH  - this hour's total vs. the average of the same hour of day on past days
 *   BUDGET       - today's total vs. the daily budget (if one is set)
 */
@Service
public class AlertService {

    private static final int MIN_SPIKE_SAMPLES = 6;
    private static final int MIN_HOURLY_SAMPLES = 3;
    private static final int SPIKE_COOLDOWN_MINUTES = 5;

    private final ReadingRepository readingRepository;
    private final UsageSummaryRepository usageSummaryRepository;
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public AlertService(ReadingRepository readingRepository,
                        UsageSummaryRepository usageSummaryRepository,
                        NotificationRepository notificationRepository,
                        Clock clock) {
        this.readingRepository = readingRepository;
        this.usageSummaryRepository = usageSummaryRepository;
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    public void check(Meter meter, Reading reading) {
        if (!meter.isAlertsEnabled()) {
            return;
        }
        checkSpike(meter, reading);
        checkHourly(meter, reading);
        checkBudget(meter, reading);
    }

    private void checkSpike(Meter meter, Reading reading) {
        List<Reading> recent = readingRepository
                .findTop30ByMeterIdAndRecordedAtLessThanOrderByRecordedAtDesc(meter.getId(), reading.getRecordedAt());
        if (recent.size() < MIN_SPIKE_SAMPLES) {
            return;
        }
        double sum = 0;
        for (Reading r : recent) {
            sum += r.getPowerWatts();
        }
        double average = sum / recent.size();
        if (average <= 0 || reading.getPowerWatts() <= average * meter.getAlertMultiplier()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (notificationRepository.existsByMeterIdAndKindAndCreatedAtAfter(
                meter.getId(), NotificationKind.SPIKE, now.minusMinutes(SPIKE_COOLDOWN_MINUTES))) {
            return;
        }
        String message = String.format(Locale.US,
                "Power reached %.0f W, which is %.1fx the recent average of %.0f W.",
                reading.getPowerWatts(), reading.getPowerWatts() / average, average);
        notify(meter, NotificationKind.SPIKE, message, now);
    }

    private void checkHourly(Meter meter, Reading reading) {
        LocalDateTime hourStart = PeriodType.HOUR.startOf(reading.getRecordedAt());
        UsageSummary current = usageSummaryRepository
                .findByMeterIdAndPeriodTypeAndPeriodStart(meter.getId(), PeriodType.HOUR, hourStart)
                .orElse(null);
        if (current == null) {
            return;
        }

        List<UsageSummary> past = usageSummaryRepository
                .findByMeterIdAndPeriodTypeAndPeriodStartBetweenOrderByPeriodStart(
                        meter.getId(), PeriodType.HOUR, hourStart.minusDays(14), hourStart.minusSeconds(1));
        double sum = 0;
        int count = 0;
        for (UsageSummary s : past) {
            if (s.getPeriodStart().getHour() == hourStart.getHour()) {
                sum += s.getTotalKwh();
                count++;
            }
        }
        if (count < MIN_HOURLY_SAMPLES) {
            return;
        }
        double average = sum / count;
        if (average <= 0 || current.getTotalKwh() <= average * meter.getAlertMultiplier()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime thisHourNow = now.truncatedTo(ChronoUnit.HOURS);
        if (notificationRepository.existsByMeterIdAndKindAndCreatedAtAfter(
                meter.getId(), NotificationKind.HOURLY_HIGH, thisHourNow.minusSeconds(1))) {
            return;
        }
        String message = String.format(Locale.US,
                "Usage in the %02d:00 hour is %.2f kWh, %.1fx the usual %.2f kWh for this hour.",
                hourStart.getHour(), current.getTotalKwh(), current.getTotalKwh() / average, average);
        notify(meter, NotificationKind.HOURLY_HIGH, message, now);
    }

    private void checkBudget(Meter meter, Reading reading) {
        if (meter.getDailyBudgetKwh() <= 0) {
            return;
        }
        LocalDateTime dayStart = PeriodType.DAY.startOf(reading.getRecordedAt());
        UsageSummary day = usageSummaryRepository
                .findByMeterIdAndPeriodTypeAndPeriodStart(meter.getId(), PeriodType.DAY, dayStart)
                .orElse(null);
        if (day == null || day.getTotalKwh() <= meter.getDailyBudgetKwh()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime todayNow = now.toLocalDate().atStartOfDay();
        if (notificationRepository.existsByMeterIdAndKindAndCreatedAtAfter(
                meter.getId(), NotificationKind.BUDGET, todayNow.minusSeconds(1))) {
            return;
        }
        String message = String.format(Locale.US,
                "Usage for the day reached %.2f kWh, over your %.2f kWh daily budget.",
                day.getTotalKwh(), meter.getDailyBudgetKwh());
        notify(meter, NotificationKind.BUDGET, message, now);
    }

    private void notify(Meter meter, NotificationKind kind, String message, LocalDateTime now) {
        Notification n = new Notification();
        n.setMeterId(meter.getId());
        n.setKind(kind);
        n.setMessage(message);
        n.setCreatedAt(now);
        n.setRead(false);
        notificationRepository.save(n);
    }
}
