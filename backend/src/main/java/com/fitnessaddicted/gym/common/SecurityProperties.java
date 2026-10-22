package com.fitnessaddicted.gym.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from {@code app.security.*} in application.yml. */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String jwtSecret, long jwtAccessTtlMinutes, long jwtRefreshTtlDays) {
}
