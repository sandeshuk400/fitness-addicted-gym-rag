package com.fitnessaddicted.gym.member.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** deviceUserId null means "unlink". */
public record UpdateDeviceUserRequest(@Min(1) @Max(9999999) Integer deviceUserId) {
}