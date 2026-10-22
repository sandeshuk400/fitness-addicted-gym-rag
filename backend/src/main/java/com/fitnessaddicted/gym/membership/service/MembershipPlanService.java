package com.fitnessaddicted.gym.membership.service;

import java.util.List;
import java.util.Map;

import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.common.AuditService;
import com.fitnessaddicted.gym.membership.api.PlanRequest;
import com.fitnessaddicted.gym.membership.api.PlanResponse;
import com.fitnessaddicted.gym.membership.domain.MembershipPlan;
import com.fitnessaddicted.gym.membership.repository.MembershipPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipPlanService {

    private final MembershipPlanRepository plans;
    private final AuditService audit;

    public MembershipPlanService(MembershipPlanRepository plans, AuditService audit) {
        this.plans = plans;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> list(boolean includeInactive) {
        List<MembershipPlan> found = includeInactive ? plans.findAllByOrderByPriceAsc()
                : plans.findByActiveTrueOrderByPriceAsc();
        return found.stream().map(PlanResponse::from).toList();
    }

    @Transactional
    public PlanResponse create(Long actorId, PlanRequest request) {
        String name = request.name().trim();
        if (plans.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("A plan with this name already exists");
        }
        MembershipPlan plan = new MembershipPlan(name, blankToNull(request.description()),
                request.durationDays(), request.price());
        if (Boolean.FALSE.equals(request.active())) {
            plan.update(name, plan.getDescription(), plan.getDurationDays(), plan.getPrice(), false);
        }
        plan = plans.save(plan);
        audit.record(actorId, "PLAN_CREATED", "MembershipPlan", String.valueOf(plan.getId()),
                Map.of("name", name));
        return PlanResponse.from(plan);
    }

    @Transactional
    public PlanResponse update(Long actorId, Long planId, PlanRequest request) {
        MembershipPlan plan = plans.findById(planId)
                .orElseThrow(() -> ApiException.notFound("Plan not found"));
        String name = request.name().trim();
        if (plans.existsByNameIgnoreCaseAndIdNot(name, planId)) {
            throw ApiException.conflict("A plan with this name already exists");
        }
        boolean active = request.active() == null ? plan.isActive() : request.active();
        plan.update(name, blankToNull(request.description()), request.durationDays(), request.price(), active);
        audit.record(actorId, "PLAN_UPDATED", "MembershipPlan", String.valueOf(planId), Map.of("name", name));
        return PlanResponse.from(plan);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
