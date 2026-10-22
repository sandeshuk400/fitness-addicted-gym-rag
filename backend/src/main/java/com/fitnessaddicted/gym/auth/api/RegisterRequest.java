package com.fitnessaddicted.gym.auth.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password,
        @NotBlank @Size(max = 150) String fullName,
        @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "Phone must be 7-20 digits, spaces, + or -") String phone,
        @AssertTrue(message = "You must accept the terms to register") boolean acceptTerms,
        boolean marketingConsent,
        boolean aiProcessingConsent) {
}
