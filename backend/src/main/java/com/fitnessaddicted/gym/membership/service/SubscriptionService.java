package com.fitnessaddicted.gym.membership.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.common.AppTime;
import com.fitnessaddicted.gym.common.AuditService;
import com.fitnessaddicted.gym.member.service.MemberProfileService;
import com.fitnessaddicted.gym.membership.api.SubscriptionResponse;
import com.fitnessaddicted.gym.membership.domain.MembershipPlan;
import com.fitnessaddicted.gym.membership.domain.Subscription;
import com.fitnessaddicted.gym.membership.domain.SubscriptionStatus;
import com.fitnessaddicted.gym.membership.repository.MembershipPlanRepository;
import com.fitnessaddicted.gym.membership.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Subscription lifecycle: buy (current or queued), freeze, unfreeze, cancel, and the daily status refresh.
 *
 * <p>Rules: a member has at most one running (ACTIVE or FROZEN) subscription; later purchases are queued
 * as PENDING and start the day after the previous one ends. Freezing extends the end date (and shifts
 * queued subscriptions) so no paid time is lost. Payments are recorded by the payment module in Phase 2.
 */
@Service
public class SubscriptionService {

    private static final Set<SubscriptionStatus> OCCUPYING_STATUSES =
            Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.FROZEN, SubscriptionStatus.PENDING);

    private final SubscriptionRepository subscriptions;
    private final MembershipPlanRepository plans;
    private final MemberProfileService members;
    private final AuditService audit;
    private final int maxFreezeDays;

    public SubscriptionService(SubscriptionRepository subscriptions, MembershipPlanRepository plans,
                               MemberProfileService members, AuditService audit,
                               @Value("${app.membership.max-freeze-days:30}") int maxFreezeDays) {
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.members = members;
        this.audit = audit;
        this.maxFreezeDays = maxFreezeDays;
    }

    /** Staff sells a plan to a member. Starts today, or the day after the member's current plan ends. */
    @Transactional
    public SubscriptionResponse purchase(Long actorId, Long memberId, Long planId) {
        if (!members.exists(memberId)) {
            throw ApiException.notFound("Member not found");
        }
        MembershipPlan plan = plans.findById(planId).orElseThrow(() -> ApiException.notFound("Plan not found"));
        if (!plan.isActive()) {
            throw ApiException.badRequest("This plan is no longer on sale");
        }
        LocalDate today = AppTime.today();
        LocalDate latestEnd = subscriptions.findLatestEndDate(memberId, OCCUPYING_STATUSES);
        LocalDate start = (latestEnd == null || latestEnd.isBefore(today)) ? today : latestEnd.plusDays(1);
        LocalDate end = start.plusDays(plan.getDurationDays() - 1L);
        SubscriptionStatus status = start.isAfter(today) ? SubscriptionStatus.PENDING : SubscriptionStatus.ACTIVE;

        Subscription saved = subscriptions.save(new Subscription(memberId, planId, start, end, status));
        audit.record(actorId, "SUBSCRIPTION_CREATED", "Subscription", String.valueOf(saved.getId()),
                Map.of("memberId", memberId, "plan", plan.getName(), "startDate", start.toString()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> listForMember(Long memberId) {
        return toResponses(subscriptions.findByMemberIdOrderByStartDateDesc(memberId));
    }

    /** Active memberships ending within the next N days, soonest first (for renewal follow-up). */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> expiringWithin(int days) {
        LocalDate today = AppTime.today();
        return toResponses(subscriptions.findByStatusAndEndDateBetweenOrderByEndDateAsc(
                SubscriptionStatus.ACTIVE, today, today.plusDays(days)));
    }

    /** True when the member has an ACTIVE membership covering today. Used by attendance check-in. */
    @Transactional(readOnly = true)
    public boolean hasActiveMembership(Long memberId) {
        LocalDate today = AppTime.today();
        return subscriptions.existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                memberId, SubscriptionStatus.ACTIVE, today, today);
    }

    @Transactional
    public SubscriptionResponse freeze(Long actorId, boolean actorIsStaff, Long subscriptionId, int days) {
        Subscription s = findAccessible(actorId, actorIsStaff, subscriptionId);
        if (s.getStatus() != SubscriptionStatus.ACTIVE) {
            throw ApiException.badRequest("Only an active membership can be frozen");
        }
        if (s.getEndDate().isBefore(AppTime.today())) {
            throw ApiException.badRequest("This membership has already ended");
        }
        int remainingAllowance = maxFreezeDays - s.getFrozenDaysUsed();
        if (days > remainingAllowance) {
            throw ApiException.badRequest(
                    "You can freeze at most " + Math.max(remainingAllowance, 0) + " more day(s) on this membership");
        }
        LocalDate oldEnd = s.getEndDate();
        s.freeze(AppTime.today().plusDays(days), days);
        shiftQueued(s.getMemberId(), oldEnd, days);
        audit.record(actorId, "SUBSCRIPTION_FROZEN", "Subscription", String.valueOf(subscriptionId),
                Map.of("days", days));
        return toResponse(s);
    }

    @Transactional
    public SubscriptionResponse unfreeze(Long actorId, boolean actorIsStaff, Long subscriptionId) {
        Subscription s = findAccessible(actorId, actorIsStaff, subscriptionId);
        if (s.getStatus() != SubscriptionStatus.FROZEN) {
            throw ApiException.badRequest("This membership is not frozen");
        }
        long unused = Math.max(0, ChronoUnit.DAYS.between(AppTime.today(), s.getFrozenUntil()));
        LocalDate oldEnd = s.getEndDate();
        s.unfreeze((int) unused);
        shiftQueued(s.getMemberId(), oldEnd, -(int) unused);
        audit.record(actorId, "SUBSCRIPTION_UNFROZEN", "Subscription", String.valueOf(subscriptionId),
                Map.of("daysGivenBack", unused));
        return toResponse(s);
    }

    /** Staff-only (controller enforces the role). Queued subscriptions keep their own start dates. */
    @Transactional
    public SubscriptionResponse cancel(Long actorId, Long subscriptionId) {
        Subscription s = subscriptions.findById(subscriptionId)
                .orElseThrow(() -> ApiException.notFound("Subscription not found"));
        if (s.getStatus() == SubscriptionStatus.CANCELLED || s.getStatus() == SubscriptionStatus.EXPIRED) {
            throw ApiException.badRequest("This membership is already " + s.getStatus().name().toLowerCase());
        }
        s.cancel();
        audit.record(actorId, "SUBSCRIPTION_CANCELLED", "Subscription", String.valueOf(subscriptionId), Map.of());
        return toResponse(s);
    }

    /**
     * Daily housekeeping, safe to run any number of times: ends finished freezes, starts queued
     * subscriptions whose date has come, then expires memberships whose end date has passed.
     * The order matters (a freeze that ends today must be ACTIVE before the expiry check).
     *
     * @return how many subscriptions changed status
     */
    @Transactional
    public int refreshStatuses() {
        LocalDate today = AppTime.today();
        int changed = subscriptions.endFreezes(SubscriptionStatus.FROZEN, SubscriptionStatus.ACTIVE, today);
        changed += subscriptions.startQueued(SubscriptionStatus.PENDING, SubscriptionStatus.ACTIVE, today);
        changed += subscriptions.expireEnded(SubscriptionStatus.ACTIVE, SubscriptionStatus.EXPIRED, today);
        return changed;
    }

    // ---------- helpers ----------

    /** Members can only reach their own subscriptions; a foreign id looks like "not found". */
    private Subscription findAccessible(Long actorId, boolean actorIsStaff, Long subscriptionId) {
        Subscription s = subscriptions.findById(subscriptionId)
                .orElseThrow(() -> ApiException.notFound("Subscription not found"));
        if (!actorIsStaff && !s.getMemberId().equals(actorId)) {
            throw ApiException.notFound("Subscription not found");
        }
        return s;
    }

    /** When a running membership's end date moves, queued ones that start after it move by the same amount. */
    private void shiftQueued(Long memberId, LocalDate oldEnd, int deltaDays) {
        if (deltaDays == 0) {
            return;
        }
        for (Subscription queued : subscriptions.findByMemberIdAndStatusAndStartDateAfter(
                memberId, SubscriptionStatus.PENDING, oldEnd)) {
            queued.shift(deltaDays);
        }
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return toResponses(List.of(s)).get(0);
    }

    private List<SubscriptionResponse> toResponses(List<Subscription> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, MembershipPlan> planById = plans.findAllById(
                        list.stream().map(Subscription::getPlanId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(MembershipPlan::getId, Function.identity()));
        Map<Long, String> names = members.fullNames(
                list.stream().map(Subscription::getMemberId).collect(Collectors.toSet()));
        LocalDate today = AppTime.today();
        return list.stream()
                .map(s -> SubscriptionResponse.from(s, planById.get(s.getPlanId()), names.get(s.getMemberId()), today))
                .toList();
    }
}
