package com.fitnessaddicted.gym.member.api;

import java.time.LocalDate;

import com.fitnessaddicted.gym.member.domain.MemberProfile;

/** healthNotes is null unless the caller is allowed to see it (the member themself or an admin). */
public record MemberResponse(Long userId, String fullName, String phone, LocalDate dateOfBirth, String gender,
                             String emergencyContact, String healthNotes, LocalDate joinedOn) {

    public static MemberResponse from(MemberProfile profile, boolean includeHealthNotes) {
        return new MemberResponse(
                profile.getUserId(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getEmergencyContact(),
                includeHealthNotes ? profile.getHealthNotes() : null,
                profile.getJoinedOn());
    }
}
