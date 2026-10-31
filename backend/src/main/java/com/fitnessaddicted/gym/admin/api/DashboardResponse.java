package com.fitnessaddicted.gym.admin.api;

import java.math.BigDecimal;
import java.time.Instant;

public record DashboardResponse(
        long totalMembers,
        long activeMembers,
        long checkInsToday,
        long insideNow,
        BigDecimal revenueThisMonth,
        long expiringIn7Days,
        Instant generatedAt) {
}