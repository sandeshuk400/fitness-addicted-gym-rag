package com.fitnessaddicted.gym.notification.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fitnessaddicted.gym.notification.api.NotificationResponse;
import com.fitnessaddicted.gym.notification.domain.Notification;
import com.fitnessaddicted.gym.notification.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine(Long userId) {
        return notifications.findByUserIdAndChannelOrderByCreatedAtDesc(userId, Notification.CHANNEL_IN_APP)
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndChannelAndReadAtIsNull(userId, Notification.CHANNEL_IN_APP);
    }

    /** Marking an already-read notification is a no-op. Other people's notifications look like "not found". */
    @Transactional
    public NotificationResponse markRead(Long userId, Long id) {
        Notification n = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.markRead();
        return NotificationResponse.from(n);
    }
}