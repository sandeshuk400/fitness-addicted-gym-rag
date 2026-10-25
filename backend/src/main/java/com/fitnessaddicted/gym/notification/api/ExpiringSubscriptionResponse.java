package com.fitnessaddicted.gym.membership.api;

import java.time.LocalDate;

import com.fitnessaddicted.gym.membership.domain.SubscriptionStatus;

/**
 * Staff-only view of a membership that is about to end. Same fields as SubscriptionResponse, plus whether
 * an expiry reminder has been sent and the most recent mark sent (7, 3 or 1 days before the end).
 */
public record ExpiringSubscriptionResponse(Long id, Long memberId, String memberName, Long planId,
                                           String planName, LocalDate startDate, LocalDate endDate,
                                           SubscriptionStatus status, LocalDate frozenUntil,
                                           int frozenDaysUsed, Long daysRemaining,
                                           boolean reminderSent, Integer lastReminderDays) {

    public static ExpiringSubscriptionResponse from(SubscriptionResponse s, Integer lastReminderDays) {
        return new ExpiringSubscriptionResponse(s.id(), s.memberId(), s.memberName(), s.planId(),
                s.planName(), s.startDate(), s.endDate(), s.status(), s.frozenUntil(),
                s.frozenDaysUsed(), s.daysRemaining(), lastReminderDays != null, lastReminderDays);
    }
}