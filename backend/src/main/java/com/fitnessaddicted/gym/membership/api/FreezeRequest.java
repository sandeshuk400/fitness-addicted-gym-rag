package com.fitnessaddicted.gym.membership.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FreezeRequest(@NotNull @Min(1) @Max(365) Integer days) {
}
