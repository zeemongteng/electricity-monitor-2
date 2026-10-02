package com.electricitymonitor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public class Dtos {

    /** Add one reading. durationSeconds defaults to 10, recordedAt defaults to now. */
    public record AddReadingRequest(
            @NotNull Long meterId,
            @NotNull @PositiveOrZero Double powerWatts,
            @Positive Integer durationSeconds,
            LocalDateTime recordedAt) {
    }

    public record HistoryRequest(
            @NotNull Long meterId,
            @Min(1) @Max(180) int days) {
    }

    public record MeterRequest(
            @NotBlank String name,
            String location,
            @PositiveOrZero Double initialReading) {
    }

    public record MeterSettings(
            @NotBlank String name,
            @Positive double alertMultiplier,
            boolean alertsEnabled,
            @PositiveOrZero double dailyBudgetKwh,
            @PositiveOrZero double ratePerKwh) {
    }

    /** One bar of a chart: usage in one hour / day / month / year. */
    public record UsagePoint(
            LocalDateTime periodStart,
            double totalKwh,
            double peakWatts,
            int recordCount) {
    }

    public record Overview(
            Long meterId,
            String name,
            double initialReading,
            double currentReading,
            double latestPowerWatts,
            LocalDateTime latestAt,
            double hourKwh,
            double todayKwh,
            double monthKwh,
            double yearKwh,
            double ratePerKwh,
            double todayCost,
            double monthCost,
            double projectedMonthKwh,
            double projectedMonthCost) {
    }

    public record SimulatorStatus(boolean autoEnabled, long intervalMs) {
    }
}
