package com.fitnessaddicted.gym.auth.api;

import com.fitnessaddicted.gym.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin-created account. Only STAFF and ADMIN for now; trainers arrive with the trainer module. */
public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password,
        @NotNull Role role) {
}
