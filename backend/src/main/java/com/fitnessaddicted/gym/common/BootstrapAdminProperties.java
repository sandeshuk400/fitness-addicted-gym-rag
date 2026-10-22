package com.fitnessaddicted.gym.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional first-admin bootstrap, bound from {@code app.bootstrap-admin.*}.
 * If both values are set and no ADMIN exists yet, an admin account is created at startup.
 */
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(String email, String password) {
}
