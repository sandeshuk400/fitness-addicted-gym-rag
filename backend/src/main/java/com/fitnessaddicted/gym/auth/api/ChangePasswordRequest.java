package com.fitnessaddicted.gym.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String newPassword) {
}
