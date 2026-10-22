package com.fitnessaddicted.gym.membership.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One purchased membership period. Dates are inclusive: a 30-day plan starting on the 1st ends on the 30th.
 * State changes go through the methods below so the date arithmetic lives in one place.
 */
@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    /** While FROZEN: the date the membership becomes ACTIVE again. */
    @Column(name = "frozen_until")
    private LocalDate frozenUntil;

    @Column(name = "frozen_days_used", nullable = false)
    private int frozenDaysUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Subscription() {
        // for JPA
    }

    public Subscription(Long memberId, Long planId, LocalDate startDate, LocalDate endDate,
                        SubscriptionStatus status) {
        this.memberId = memberId;
        this.planId = planId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    /** Freezing pushes the end date out by the same number of days, so no paid time is lost. */
    public void freeze(LocalDate until, int days) {
        this.status = SubscriptionStatus.FROZEN;
        this.frozenUntil = until;
        this.endDate = endDate.plusDays(days);
        this.frozenDaysUsed += days;
    }

    /** Ending a freeze early gives back the days that were not used. */
    public void unfreeze(int unusedDays) {
        this.status = SubscriptionStatus.ACTIVE;
        this.frozenUntil = null;
        this.endDate = endDate.minusDays(unusedDays);
        this.frozenDaysUsed -= unusedDays;
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
        this.frozenUntil = null;
    }

    /** Moves a queued subscription earlier or later (negative days = earlier). */
    public void shift(int days) {
        this.startDate = startDate.plusDays(days);
        this.endDate = endDate.plusDays(days);
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getPlanId() {
        return planId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public LocalDate getFrozenUntil() {
        return frozenUntil;
    }

    public int getFrozenDaysUsed() {
        return frozenDaysUsed;
    }
}
