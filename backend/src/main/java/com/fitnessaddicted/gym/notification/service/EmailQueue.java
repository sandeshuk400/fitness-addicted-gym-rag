package com.fitnessaddicted.gym.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fitnessaddicted.gym.notification.domain.Notification;
import com.fitnessaddicted.gym.notification.repository.NotificationRepository;

/**
 * Queues an email copy of an in-app notice. Does nothing while app.notifications.email.enabled is false,
 * so rows do not pile up when email is switched off. Call it in the same transaction that saved the notice.
 */
@Component
public class EmailQueue {

    private final NotificationRepository notifications;
    private final boolean enabled;

    public EmailQueue(NotificationRepository notifications,
                      @Value("${app.notifications.email.enabled:false}") boolean enabled) {
        this.notifications = notifications;
        this.enabled = enabled;
    }

    public void queueCopy(Notification inAppNotice) {
        if (enabled) {
            notifications.save(Notification.emailCopyOf(inAppNotice));
        }
    }
}