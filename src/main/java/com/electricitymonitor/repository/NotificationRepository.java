package com.electricitymonitor.repository;

import com.electricitymonitor.model.Notification;
import com.electricitymonitor.model.NotificationKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByOrderByCreatedAtDesc();

    boolean existsByMeterIdAndKindAndCreatedAtAfter(Long meterId, NotificationKind kind, LocalDateTime after);
}
