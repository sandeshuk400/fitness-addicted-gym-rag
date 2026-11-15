package com.fitnessaddicted.gym.member.domain;

/** Purposes a member can consent to separately (purpose-specific consent, in the spirit of DPDP). */
public enum ConsentPurpose {
    TERMS,
    HEALTH_DATA,
    MARKETING,
    AI_PROCESSING,
    
    /** Using a fingerprint, stored on the door device only, to open the gym door. */
    BIOMETRIC_DOOR
}


