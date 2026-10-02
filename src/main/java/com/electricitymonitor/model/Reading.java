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

/** One raw measurement: average power over a short interval. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Reading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meterId;
    private LocalDateTime recordedAt;

    /** Average power during the interval. */
    private double powerWatts;
    private int durationSeconds;

    /** Energy used during the interval = powerWatts x durationSeconds / 3,600,000. */
    private double usedKwh;

    /** Meter reading right after this interval. */
    private double cumulativeKwh;

    @Enumerated(EnumType.STRING)
    private ReadingSource source;
}
