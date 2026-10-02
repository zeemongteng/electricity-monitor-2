package com.electricitymonitor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Meter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;

    /** Meter reading (kWh) when it was installed. Never changes. */
    private double initialReading;

    /** Cumulative reading (kWh) right now = initialReading + all usage so far. */
    private double currentReading;

    /** Alert when usage is more than this many times the average. */
    private double alertMultiplier = 1.5;
    private boolean alertsEnabled = true;

    /** Daily budget in kWh. 0 means no budget alert. */
    private double dailyBudgetKwh = 0;

    /** Price per kWh in baht, used for the cost estimate. */
    private double ratePerKwh = 4.0;

    private LocalDateTime createdAt;
}
