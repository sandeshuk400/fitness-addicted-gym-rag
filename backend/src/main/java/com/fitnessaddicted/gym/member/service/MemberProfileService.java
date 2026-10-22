package com.fitnessaddicted.gym.member.service;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.common.AuditService;
import com.fitnessaddicted.gym.common.PageResponse;
import com.fitnessaddicted.gym.member.api.MemberResponse;
import com.fitnessaddicted.gym.member.api.UpdateMemberRequest;
import com.fitnessaddicted.gym.member.domain.MemberProfile;
import com.fitnessaddicted.gym.member.repository.MemberProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberProfileService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MemberProfileRepository repository;
    private final AuditService audit;

    public MemberProfileService(MemberProfileRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    /** Called by the auth module when a member registers. */
    @Transactional
    public void createProfile(Long userId, String fullName, String phone) {
        repository.save(new MemberProfile(userId, fullName, phone));
    }

    /** Used by other modules (membership, attendance) to validate a member id. */
    @Transactional(readOnly = true)
    public boolean exists(Long userId) {
        return repository.existsById(userId);
    }

    /** Used by other modules to show member names in lists without loading whole profiles. */
    @Transactional(readOnly = true)
    public Map<Long, String> fullNames(Collection<Long> userIds) {
        return repository.findAllById(userIds).stream()
                .collect(Collectors.toMap(MemberProfile::getUserId, MemberProfile::getFullName));
    }

    @Transactional(readOnly = true)
    public MemberResponse getOwn(Long userId) {
        return MemberResponse.from(find(userId), true);
    }

    @Transactional
    public MemberResponse updateOwn(Long userId, UpdateMemberRequest request) {
        MemberProfile profile = find(userId);
        String newNotes = blankToNull(request.healthNotes());
        boolean notesChanged = !Objects.equals(profile.getHealthNotes(), newNotes);
        profile.update(
                request.fullName().trim(),
                blankToNull(request.phone()),
                request.dateOfBirth(),
                blankToNull(request.gender()),
                blankToNull(request.emergencyContact()),
                newNotes);
        if (notesChanged) {
            audit.record(userId, "HEALTH_NOTES_UPDATED", "MemberProfile", String.valueOf(userId), Map.of());
        }
        return MemberResponse.from(profile, true);
    }

    /**
     * Staff/admin lookup. Health notes are only included for admins, and each such view is audited.
     * Not read-only because it may write an audit row.
     */
    @Transactional
    public MemberResponse getForStaff(Long actorId, boolean actorIsAdmin, Long memberId) {
        MemberProfile profile = find(memberId);
        if (actorIsAdmin && profile.getHealthNotes() != null) {
            audit.record(actorId, "HEALTH_NOTES_VIEWED", "MemberProfile", String.valueOf(memberId), Map.of());
        }
        return MemberResponse.from(profile, actorIsAdmin);
    }

    /** Staff/admin search by name or phone. Never includes health notes. */
    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> search(String query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("fullName"));
        Page<MemberProfile> result;
        if (query == null || query.isBlank()) {
            result = repository.findAll(pageable);
        } else {
            String q = query.trim();
            result = repository.findByFullNameContainingIgnoreCaseOrPhoneContaining(q, q, pageable);
        }
        return PageResponse.from(result.map(profile -> MemberResponse.from(profile, false)));
    }

    private MemberProfile find(Long userId) {
        return repository.findById(userId).orElseThrow(() -> ApiException.notFound("Member not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
