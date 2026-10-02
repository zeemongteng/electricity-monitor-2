package com.electricitymonitor.repository;

import com.electricitymonitor.model.Reading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReadingRepository extends JpaRepository<Reading, Long> {

    List<Reading> findTop30ByMeterIdAndRecordedAtLessThanOrderByRecordedAtDesc(Long meterId, LocalDateTime before);

    List<Reading> findTop50ByMeterIdOrderByRecordedAtDesc(Long meterId);

    Optional<Reading> findFirstByMeterIdOrderByRecordedAtDesc(Long meterId);

    @Modifying
    @Query("delete from Reading r where r.recordedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
