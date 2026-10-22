package com.fitnessaddicted.gym.membership.api;

import java.util.List;

import com.fitnessaddicted.gym.common.CurrentUser;
import com.fitnessaddicted.gym.membership.service.MembershipPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/membership-plans")
public class MembershipPlanController {

    private final MembershipPlanService planService;

    public MembershipPlanController(MembershipPlanService planService) {
        this.planService = planService;
    }

    /** Plans currently on sale, cheapest first. Any signed-in user. */
    @GetMapping
    public List<PlanResponse> list() {
        return planService.list(false);
    }

    /** Every plan including withdrawn ones. */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<PlanResponse> listAll() {
        return planService.list(true);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlanRequest request) {
        return planService.create(CurrentUser.id(jwt), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PlanResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody PlanRequest request) {
        return planService.update(CurrentUser.id(jwt), id, request);
    }
}
