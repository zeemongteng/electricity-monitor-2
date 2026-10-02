package com.electricitymonitor.controller;

import com.electricitymonitor.model.Notification;
import com.electricitymonitor.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** The 50 newest notifications, newest first. */
    @GetMapping
    public List<Notification> list(@RequestParam(defaultValue = "false") boolean unreadOnly) {
        List<Notification> all = notificationRepository.findTop50ByOrderByCreatedAtDesc();
        if (!unreadOnly) {
            return all;
        }
        List<Notification> unread = new ArrayList<>();
        for (Notification n : all) {
            if (!n.isRead()) {
                unread.add(n);
            }
        }
        return unread;
    }

    @PatchMapping("/{id}/read")
    public Notification markRead(@PathVariable Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.setRead(true);
        return notificationRepository.save(n);
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        List<Notification> all = notificationRepository.findAll();
        for (Notification n : all) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        }
    }
}
