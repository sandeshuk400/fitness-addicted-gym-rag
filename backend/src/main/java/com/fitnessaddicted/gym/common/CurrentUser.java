package com.fitnessaddicted.gym.common;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

/** Helpers for reading the authenticated user from the validated JWT. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public static boolean hasRole(Jwt jwt, String role) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains(role);
    }

    /** True for front-desk staff and admins (the people allowed to act on other members' records). */
    public static boolean isStaff(Jwt jwt) {
        return hasRole(jwt, "STAFF") || hasRole(jwt, "ADMIN");
    }
}
