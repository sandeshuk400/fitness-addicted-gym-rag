package com.fitnessaddicted.gym.membership.api;

import jakarta.validation.constraints.NotNull;

public record PurchaseRequest(@NotNull Long planId) {
}
