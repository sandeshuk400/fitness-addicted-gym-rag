package com.fitnessaddicted.gym.auth.api;

import com.fitnessaddicted.gym.auth.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull UserStatus status) {
}
