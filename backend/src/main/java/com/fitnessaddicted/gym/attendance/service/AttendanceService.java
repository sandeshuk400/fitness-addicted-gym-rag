package com.fitnessaddicted.gym.attendance.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.attendance.api.AttendanceResponse;
import com.fitnessaddicted.gym.attendance.api.OccupancyResponse;
import com.fitnessaddicted.gym.attendance.domain.Attendance;
import com.fitnessaddicted.gym.attendance.domain.AttendanceSource;
import com.fitnessaddicted.gym.attendance.repository.AttendanceRepository;
import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.common.AppTime;
import com.fitnessaddicted.gym.common.PageResponse;
import com.fitnessaddicted.gym.member.service.MemberProfileService;
import com.fitnessaddicted.gym.membership.service.SubscriptionService;

/**
 * Check-in and check-out. A member can only enter with an ACTIVE membership covering today
 * (frozen, expired, cancelled and not-yet-started memberships are refused).
 *
 * <p>Members forget to check out, so an open visit older than {@link #OPEN_VISIT_MAX_AGE} is treated as
 * a forgotten check-out: it no longer counts as "inside" and does not block a new check-in.
 * The row itself is left untouched rather than inventing a check-out time.
 */
@Service
public class AttendanceService {

    static final Duration OPEN_VISIT_MAX_AGE = Duration.ofHours(12);
    private static final int MAX_PAGE_SIZE = 100;
    private static final LocalTime OPENS = LocalTime.of(5, 0);
    private static final LocalTime CLOSES = LocalTime.of(23, 10);
    private final AttendanceRepository attendance;
    private final MemberProfileService members;
    private final SubscriptionService subscriptions;

    public AttendanceService(AttendanceRepository attendance, MemberProfileService members,
                             SubscriptionService subscriptions) {
        this.attendance = attendance;
        this.members = members;
        this.subscriptions = subscriptions;
    }

    @Transactional
    public AttendanceResponse checkIn(Long memberId) {
    LocalTime now = LocalTime.now(AppTime.GYM_ZONE);
        if (now.isBefore(OPENS) || !now.isBefore(CLOSES)) {
            throw ApiException.badRequest("The gym is open from 5:00 am to 11:00 pm");
        }
        if (!members.exists(memberId)) {
            throw ApiException.notFound("Member not found");
        }
        if (!subscriptions.hasActiveMembership(memberId)) {
            throw ApiException.forbidden("This member has no active membership");
        }
        if (findOpenVisit(memberId) != null) {
            throw ApiException.conflict("Member is already checked in");
        }
        return AttendanceResponse.from(attendance.save(new Attendance(memberId, AttendanceSource.FRONT_DESK)));
    }

    @Transactional
    public AttendanceResponse checkOut(Long memberId) {
        Attendance open = findOpenVisit(memberId);
        if (open == null) {
            throw ApiException.conflict("Member is not checked in");
        }
        open.checkOut(Instant.now());
        return AttendanceResponse.from(open);
    }

    @Transactional(readOnly = true)
    public OccupancyResponse occupancy() {
        Instant now = Instant.now();
        return new OccupancyResponse(attendance.countByCheckOutIsNullAndCheckInAfter(now.minus(OPEN_VISIT_MAX_AGE)), now);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> history(Long memberId, int page, int size) {
        Page<Attendance> result = attendance.findByMemberIdOrderByCheckInDesc(memberId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE)));
        return PageResponse.from(result.map(AttendanceResponse::from));
    }

    private Attendance findOpenVisit(Long memberId) {
        return attendance.findFirstByMemberIdAndCheckOutIsNullAndCheckInAfterOrderByCheckInDesc(
                memberId, Instant.now().minus(OPEN_VISIT_MAX_AGE)).orElse(null);
    }
}
