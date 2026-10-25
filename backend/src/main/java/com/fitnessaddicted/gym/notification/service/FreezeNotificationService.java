package com.fitnessaddicted.gym.notification.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.membership.domain.Subscription;
import com.fitnessaddicted.gym.membership.domain.SubscriptionStatus;
import com.fitnessaddicted.gym.membership.repository.SubscriptionRepository;
import com.fitnessaddicted.gym.notification.domain.Notification;
import com.fitnessaddicted.gym.notification.repository.NotificationRepository;

/**
 * Tells members their freeze has ended. Must run just before the freezes are switched back to ACTIVE,
 * in the same transaction: once the status changes, these subscriptions no longer match, so each
 * freeze produces exactly one notice and a rerun finds nothing.
 */
@Service
public class FreezeNotificationService {

    private final SubscriptionRepository subscriptions;
    private final NotificationRepository notifications;
    private final EmailQueue emailQueue;

    public FreezeNotificationService(SubscriptionRepository subscriptions, NotificationRepository notifications,
                                     EmailQueue emailQueue) {
        this.subscriptions = subscriptions;
        this.notifications = notifications;
        this.emailQueue = emailQueue;
    }

    @Transactional
    public int notifyFreezesEnding(LocalDate today) {
        int created = 0;
        for (Subscription s : subscriptions.findByStatusAndFrozenUntilLessThanEqual(
                SubscriptionStatus.FROZEN, today)) {
            Notification notice = notifications.save(Notification.freezeEnded(s.getMemberId(), s.getId(),
                    "Freeze ended",
                    "Your membership freeze has ended and your membership is active again. "
                            + "It now runs until " + s.getEndDate() + "."));
            emailQueue.queueCopy(notice);
            created++;
        }
        return created;
    }
}