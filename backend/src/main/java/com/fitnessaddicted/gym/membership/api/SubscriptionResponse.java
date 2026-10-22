package com.fitnessaddicted.gym.membership.api;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.fitnessaddicted.gym.membership.domain.MembershipPlan;
import com.fitnessaddicted.gym.membership.domain.Subscription;
import com.fitnessaddicted.gym.membership.domain.SubscriptionStatus;

/** daysRemaining counts today and the end date, and is null unless the membership is ACTIVE or FROZEN. */
public record SubscriptionResponse(Long id, Long memberId, String memberName, Long planId, String planName,
                                   LocalDate startDate, LocalDate endDate, SubscriptionStatus status,
                                   LocalDate frozenUntil, int frozenDaysUsed, Long daysRemaining) {

    public static SubscriptionResponse from(Subscription s, MembershipPlan plan, String memberName,
                                            LocalDate today) {
        boolean running = s.getStatus() == SubscriptionStatus.ACTIVE || s.getStatus() == SubscriptionStatus.FROZEN;
        Long remaining = running ? Math.max(0, ChronoUnit.DAYS.between(today, s.getEndDate()) + 1) : null;
        return new SubscriptionResponse(s.getId(), s.getMemberId(), memberName, s.getPlanId(),
                plan == null ? null : plan.getName(), s.getStartDate(), s.getEndDate(), s.getStatus(),
                s.getFrozenUntil(), s.getFrozenDaysUsed(), remaining);
    }
}
