package com.fitnessaddicted.gym.attendance.api;

import jakarta.validation.constraints.NotNull;

/** Used for both check-in and check-out at the front desk. */
public record CheckInRequest(@NotNull Long memberId) {
}
