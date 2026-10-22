package com.fitnessaddicted.gym.membership.api;

import java.math.BigDecimal;

import com.fitnessaddicted.gym.membership.domain.MembershipPlan;

public record PlanResponse(Long id, String name, String description, int durationDays, BigDecimal price,
                           boolean active) {

    public static PlanResponse from(MembershipPlan plan) {
        return new PlanResponse(plan.getId(), plan.getName(), plan.getDescription(), plan.getDurationDays(),
                plan.getPrice(), plan.isActive());
    }
}
