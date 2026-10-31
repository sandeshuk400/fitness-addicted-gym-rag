
package com.fitnessaddicted.gym.admin.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.admin.api.DashboardResponse;
import com.fitnessaddicted.gym.attendance.repository.AttendanceRepository;
import com.fitnessaddicted.gym.common.AppTime;
import com.fitnessaddicted.gym.member.repository.MemberProfileRepository;
import com.fitnessaddicted.gym.membership.domain.SubscriptionStatus;
import com.fitnessaddicted.gym.membership.repository.SubscriptionRepository;
import com.fitnessaddicted.gym.payment.domain.PaymentStatus;
import com.fitnessaddicted.gym.payment.repository.PaymentRepository;

/** Headline numbers for the staff home page. All "today" and "this month" bounds use gym time. */
@Service
public class DashboardService {

    private static final Duration OPEN_VISIT_MAX_AGE = Duration.ofHours(12); // same rule as occupancy

    private final MemberProfileRepository members;
    private final SubscriptionRepository subscriptions;
    private final AttendanceRepository attendance;
    private final PaymentRepository payments;

    public DashboardService(MemberProfileRepository members, SubscriptionRepository subscriptions,
                            AttendanceRepository attendance, PaymentRepository payments) {
        this.members = members;
        this.subscriptions = subscriptions;
        this.attendance = attendance;
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        LocalDate today = AppTime.today();
        Instant now = Instant.now();
        Instant dayStart = today.atStartOfDay(AppTime.GYM_ZONE).toInstant();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        Instant monthStart = firstOfMonth.atStartOfDay(AppTime.GYM_ZONE).toInstant();
        Instant nextMonthStart = firstOfMonth.plusMonths(1).atStartOfDay(AppTime.GYM_ZONE).toInstant();

        BigDecimal revenue = payments.sumAmountBetween(PaymentStatus.PAID, monthStart, nextMonthStart);

        return new DashboardResponse(
                members.count(),
                subscriptions.countMembersCovered(SubscriptionStatus.ACTIVE, today),
                attendance.countByCheckInGreaterThanEqual(dayStart),
                attendance.countByCheckOutIsNullAndCheckInAfter(now.minus(OPEN_VISIT_MAX_AGE)),
                revenue == null ? BigDecimal.ZERO : revenue,
                subscriptions.countByStatusAndEndDateBetween(SubscriptionStatus.ACTIVE, today, today.plusDays(7)),
                now);
    }
}