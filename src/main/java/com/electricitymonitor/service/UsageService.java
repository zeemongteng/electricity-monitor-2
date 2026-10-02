package com.electricitymonitor.service;

import com.electricitymonitor.dto.Dtos.Overview;
import com.electricitymonitor.dto.Dtos.UsagePoint;
import com.electricitymonitor.model.Meter;
import com.electricitymonitor.model.PeriodType;
import com.electricitymonitor.model.Reading;
import com.electricitymonitor.model.UsageSummary;
import com.electricitymonitor.repository.ReadingRepository;
import com.electricitymonitor.repository.UsageSummaryRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Keeps the hour / day / month / year totals and reads them back for the dashboard. */
@Service
public class UsageService {

    private final UsageSummaryRepository usageSummaryRepository;
    private final ReadingRepository readingRepository;
    private final Clock clock;

    public UsageService(UsageSummaryRepository usageSummaryRepository,
                        ReadingRepository readingRepository,
                        Clock clock) {
        this.usageSummaryRepository = usageSummaryRepository;
        this.readingRepository = readingRepository;
        this.clock = clock;
    }

    /** Add one reading's energy to its hour, day, month and year. */
    public void addUsage(Long meterId, LocalDateTime time, double kwh, double watts) {
        for (PeriodType type : PeriodType.values()) {
            LocalDateTime start = type.startOf(time);
            UsageSummary summary = usageSummaryRepository
                    .findByMeterIdAndPeriodTypeAndPeriodStart(meterId, type, start)
                    .orElse(null);
            if (summary == null) {
                summary = new UsageSummary();
                summary.setMeterId(meterId);
                summary.setPeriodType(type);
                summary.setPeriodStart(start);
            }
            summary.setTotalKwh(summary.getTotalKwh() + kwh);
            summary.setPeakWatts(Math.max(summary.getPeakWatts(), watts));
            summary.setRecordCount(summary.getRecordCount() + 1);
            usageSummaryRepository.save(summary);
        }
    }

    /** The last `count` periods ending now. Periods with no data are returned as zero. */
    public List<UsagePoint> series(Long meterId, PeriodType type, int count) {
        LocalDateTime last = type.startOf(LocalDateTime.now(clock));
        LocalDateTime first = type.plus(last, -(count - 1));
        List<UsageSummary> rows = usageSummaryRepository
                .findByMeterIdAndPeriodTypeAndPeriodStartBetweenOrderByPeriodStart(meterId, type, first, last);

        List<UsagePoint> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LocalDateTime start = type.plus(first, i);
            UsageSummary match = null;
            for (UsageSummary row : rows) {
                if (row.getPeriodStart().equals(start)) {
                    match = row;
                    break;
                }
            }
            if (match == null) {
                points.add(new UsagePoint(start, 0, 0, 0));
            } else {
                points.add(new UsagePoint(start, match.getTotalKwh(), match.getPeakWatts(), match.getRecordCount()));
            }
        }
        return points;
    }

    public Overview overview(Meter meter) {
        LocalDateTime now = LocalDateTime.now(clock);
        Long id = meter.getId();

        double hour = totalFor(id, PeriodType.HOUR, now);
        double today = totalFor(id, PeriodType.DAY, now);
        double month = totalFor(id, PeriodType.MONTH, now);
        double year = totalFor(id, PeriodType.YEAR, now);

        Reading latest = readingRepository.findFirstByMeterIdOrderByRecordedAtDesc(id).orElse(null);
        double latestWatts = 0;
        LocalDateTime latestAt = null;
        if (latest != null) {
            latestWatts = latest.getPowerWatts();
            latestAt = latest.getRecordedAt();
        }

        // Straight-line projection: usage so far this month / share of the month that has passed.
        LocalDateTime monthStart = PeriodType.MONTH.startOf(now);
        double elapsed = Duration.between(monthStart, now).getSeconds();
        double whole = Duration.between(monthStart, monthStart.plusMonths(1)).getSeconds();
        double projected = month;
        if (elapsed > 3600) {
            projected = month * whole / elapsed;
        }

        double rate = meter.getRatePerKwh();
        return new Overview(id, meter.getName(), meter.getInitialReading(), meter.getCurrentReading(),
                latestWatts, latestAt, hour, today, month, year, rate,
                today * rate, month * rate, projected, projected * rate);
    }

    private double totalFor(Long meterId, PeriodType type, LocalDateTime now) {
        UsageSummary summary = usageSummaryRepository
                .findByMeterIdAndPeriodTypeAndPeriodStart(meterId, type, type.startOf(now))
                .orElse(null);
        if (summary == null) {
            return 0;
        }
        return summary.getTotalKwh();
    }
}
