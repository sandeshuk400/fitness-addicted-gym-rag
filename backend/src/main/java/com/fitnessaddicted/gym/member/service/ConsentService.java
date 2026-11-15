package com.fitnessaddicted.gym.member.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.common.AuditService;
import com.fitnessaddicted.gym.member.api.ConsentResponse;
import com.fitnessaddicted.gym.member.domain.ConsentPurpose;
import com.fitnessaddicted.gym.member.domain.ConsentRecord;
import com.fitnessaddicted.gym.member.repository.ConsentRecordRepository;
import com.fitnessaddicted.gym.member.repository.MemberProfileRepository;

@Service
public class ConsentService {

    private final ConsentRecordRepository repository;
    private final MemberProfileRepository profiles;
    private final AuditService audit;

    public ConsentService(ConsentRecordRepository repository, MemberProfileRepository profiles,
                          AuditService audit) {
        this.repository = repository;
        this.profiles = profiles;
        this.audit = audit;
    }

    /** Appends a consent decision. Used internally (e.g. at registration). */
    @Transactional
    public ConsentResponse record(Long userId, ConsentPurpose purpose, boolean granted) {
        ConsentRecord saved = repository.save(new ConsentRecord(userId, purpose, granted));
        return new ConsentResponse(saved.getPurpose(), saved.isGranted(), saved.getRecordedAt());
    }

    /** Member-initiated change from the API. Terms acceptance cannot be withdrawn this way. */
    @Transactional
    public ConsentResponse update(Long userId, ConsentPurpose purpose, boolean granted) {
        if (purpose == ConsentPurpose.TERMS && !granted) {
            throw ApiException.badRequest("Terms acceptance cannot be withdrawn here; contact the gym to close your account");
        }
        ConsentResponse response = record(userId, purpose, granted);
        audit.record(userId, "CONSENT_CHANGED", "ConsentRecord", purpose.name(), Map.of("granted", granted));
        if (purpose == ConsentPurpose.BIOMETRIC_DOOR && !granted) {
            // Withdrawn consent ends the link to the door device. Staff must also delete the
            // fingerprint on the device itself; the app cannot do that yet.
            profiles.findById(userId).ifPresent(profile -> {
                if (profile.getDeviceUserId() != null) {
                    profile.setDeviceUserId(null);
                    audit.record(userId, "DEVICE_USER_CLEARED", "MemberProfile", String.valueOf(userId),
                            Map.of("reason", "biometric consent withdrawn"));
                }
            });
        }
        return response;
    }

    /** True only if the member's latest decision for this purpose is a grant. */
    @Transactional(readOnly = true)
    public boolean isGranted(Long userId, ConsentPurpose purpose) {
        return repository.findFirstByUserIdAndPurposeOrderByRecordedAtDescIdDesc(userId, purpose)
                .map(ConsentRecord::isGranted)
                .orElse(false);
    }

    /** Latest decision per purpose; purposes with no decision yet are reported as not granted. */
    @Transactional(readOnly = true)
    public List<ConsentResponse> current(Long userId) {
        return Arrays.stream(ConsentPurpose.values())
                .map(purpose -> repository.findFirstByUserIdAndPurposeOrderByRecordedAtDescIdDesc(userId, purpose)
                        .map(r -> new ConsentResponse(r.getPurpose(), r.isGranted(), r.getRecordedAt()))
                        .orElseGet(() -> new ConsentResponse(purpose, false, null)))
                .toList();
    }
}