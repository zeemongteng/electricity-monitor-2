package com.electricitymonitor.repository;

import com.electricitymonitor.model.Meter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeterRepository extends JpaRepository<Meter, Long> {
}
