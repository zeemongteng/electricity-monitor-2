package com.electricitymonitor.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public enum PeriodType {
    HOUR, DAY, MONTH, YEAR;

    /** Start of the period that contains time t. */
    public LocalDateTime startOf(LocalDateTime t) {
        switch (this) {
            case HOUR:
                return t.truncatedTo(ChronoUnit.HOURS);
            case DAY:
                return t.toLocalDate().atStartOfDay();
            case MONTH:
                return t.toLocalDate().withDayOfMonth(1).atStartOfDay();
            default:
                return LocalDate.of(t.getYear(), 1, 1).atStartOfDay();
        }
    }

    /** Move a period start forward (or backward if n is negative) by n periods. */
    public LocalDateTime plus(LocalDateTime start, long n) {
        switch (this) {
            case HOUR:
                return start.plusHours(n);
            case DAY:
                return start.plusDays(n);
            case MONTH:
                return start.plusMonths(n);
            default:
                return start.plusYears(n);
        }
    }
}
