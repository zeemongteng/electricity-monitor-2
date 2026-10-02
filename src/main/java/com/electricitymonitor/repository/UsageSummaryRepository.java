package com.electricitymonitor.repository;

import com.electricitymonitor.model.PeriodType;
import com.electricitymonitor.model.UsageSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsageSummaryRepository extends JpaRepository<UsageSummary, Long> {

    Optional<UsageSummary> findByMeterIdAndPeriodTypeAndPeriodStart(Long meterId, PeriodType periodType, LocalDateTime periodStart);

    List<UsageSummary> findByMeterIdAndPeriodTypeAndPeriodStartBetweenOrderByPeriodStart(
            Long meterId, PeriodType periodType, LocalDateTime from, LocalDateTime to);

    Optional<UsageSummary> findFirstByMeterIdAndPeriodTypeOrderByPeriodStartAsc(Long meterId, PeriodType periodType);
}
