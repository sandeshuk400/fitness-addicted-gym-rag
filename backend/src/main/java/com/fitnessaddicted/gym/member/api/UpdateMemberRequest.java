package com.fitnessaddicted.gym.member.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMemberRequest(
        @NotBlank @Size(max = 150) String fullName,
        @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "Phone must be 7-20 digits, spaces, + or -") String phone,
        @Past LocalDate dateOfBirth,
        @Size(max = 20) String gender,
        @Size(max = 150) String emergencyContact,
        @Size(max = 5000) String healthNotes) {
}
