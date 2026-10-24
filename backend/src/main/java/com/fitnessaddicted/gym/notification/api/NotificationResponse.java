package com.fitnessaddicted.gym.notification.api;

import java.time.Instant;

import com.fitnessaddicted.gym.notification.domain.Notification;

public record NotificationResponse(Long id, String type, String title, String message, Integer daysBefore,
                                   Long subscriptionId, Instant createdAt, Instant readAt, boolean read) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getDaysBefore(), n.getSubscriptionId(), n.getCreatedAt(), n.getReadAt(),
                n.getReadAt() != null);
    }
}