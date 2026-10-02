package com.electricitymonitor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Total usage for one hour, day, month or year of one meter. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class UsageSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meterId;

    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    /** First moment of the period, e.g. 2026-10-02T14:00 for an hour. */
    private LocalDateTime periodStart;

    private double totalKwh;
    private double peakWatts;
    private int recordCount;
}
