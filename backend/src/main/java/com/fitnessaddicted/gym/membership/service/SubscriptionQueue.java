package com.fitnessaddicted.gym.membership.service;

import java.time.LocalDate;
import java.util.Set;

import com.fitnessaddicted.gym.common.AppTime;
import com.fitnessaddicted.gym.membership.domain.Subscription;
import com.fitnessaddicted.gym.membership.domain.SubscriptionStatus;
import com.fitnessaddicted.gym.membership.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-packs a member's queued (PENDING) subscriptions so they follow directly after whatever is still
 * running. Called after a cancel or refund so the member is never left with a gap.
 */
@Component
public class SubscriptionQueue {

    private static final Set<SubscriptionStatus> RUNNING =
            Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.FROZEN);

    private final SubscriptionRepository subscriptions;

    public SubscriptionQueue(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Transactional
    public void compact(Long memberId) {
        LocalDate today = AppTime.today();
        LocalDate runningEnd = subscriptions.findLatestEndDate(memberId, RUNNING);
        LocalDate next = (runningEnd == null || runningEnd.isBefore(today)) ? today : runningEnd.plusDays(1);
        for (Subscription queued : subscriptions.findByMemberIdAndStatusOrderByStartDateAsc(
                memberId, SubscriptionStatus.PENDING)) {
            queued.reschedule(next, next.isAfter(today) ? SubscriptionStatus.PENDING : SubscriptionStatus.ACTIVE);
            next = queued.getEndDate().plusDays(1);
        }
    }
}