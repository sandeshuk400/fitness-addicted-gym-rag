package com.fitnessaddicted.gym.auth.api;

import java.time.Instant;

import com.fitnessaddicted.gym.auth.domain.AppUser;
import com.fitnessaddicted.gym.auth.domain.Role;
import com.fitnessaddicted.gym.auth.domain.UserStatus;

public record UserResponse(Long id, String email, Role role, UserStatus status, Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
