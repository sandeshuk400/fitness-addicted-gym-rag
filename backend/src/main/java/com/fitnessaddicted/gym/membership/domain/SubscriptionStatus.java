package com.fitnessaddicted.gym.membership.domain;

public enum SubscriptionStatus {
    /** Bought, but its start date is still in the future (queued behind the current one). */
    PENDING,
    ACTIVE,
    FROZEN,
    EXPIRED,
    CANCELLED
}
