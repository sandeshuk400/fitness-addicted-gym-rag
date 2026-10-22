package com.fitnessaddicted.gym.member.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.common.AuditService;
import com.fitnessaddicted.gym.member.api.ConsentResponse;
import com.fitnessaddicted.gym.member.domain.ConsentPurpose;
import com.fitnessaddicted.gym.member.domain.ConsentRecord;
import com.fitnessaddicted.gym.member.repository.ConsentRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentService {

    private final ConsentRecordRepository repository;
    private final AuditService audit;

    public ConsentService(ConsentRecordRepository repository, AuditService audit) {
        this.repository = repository;
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
        return response;
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
