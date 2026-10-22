package com.fitnessaddicted.gym.member.api;

import java.time.Instant;

import com.fitnessaddicted.gym.member.domain.ConsentPurpose;

/** recordedAt is null when the member has never made a choice for this purpose (treated as not granted). */
public record ConsentResponse(ConsentPurpose purpose, boolean granted, Instant recordedAt) {
}
