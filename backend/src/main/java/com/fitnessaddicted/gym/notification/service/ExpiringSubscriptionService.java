package com.fitnessaddicted.gym.membership.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.membership.api.ExpiringSubscriptionResponse;
import com.fitnessaddicted.gym.membership.api.SubscriptionResponse;
import com.fitnessaddicted.gym.notification.domain.Notification;
import com.fitnessaddicted.gym.notification.repository.NotificationRepository;

/** The staff renewal list: memberships ending soon, with whether each member has been reminded yet. */
@Service
public class ExpiringSubscriptionService {

    private final SubscriptionService subscriptions;
    private final NotificationRepository notifications;

    public ExpiringSubscriptionService(SubscriptionService subscriptions, NotificationRepository notifications) {
        this.subscriptions = subscriptions;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<ExpiringSubscriptionResponse> expiringWithin(int days) {
        List<SubscriptionResponse> list = subscriptions.expiringWithin(days);
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = list.stream().map(SubscriptionResponse::id).collect(Collectors.toSet());
        // One query for the whole list. The smallest mark sent is the most recent one (7, then 3, then 1).
        Map<Long, Integer> lastMark = new HashMap<>();
        for (Object[] row : notifications.findLastReminderMarks(ids, Notification.TYPE_SUBSCRIPTION_EXPIRY,
                Notification.CHANNEL_IN_APP)) {
            lastMark.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return list.stream()
                .map(s -> ExpiringSubscriptionResponse.from(s, lastMark.get(s.id())))
                .toList();
    }
}