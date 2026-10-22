package com.fitnessaddicted.gym.attendance.api;

import java.time.Instant;

public record OccupancyResponse(long currentlyInGym, Instant asOf) {
}
