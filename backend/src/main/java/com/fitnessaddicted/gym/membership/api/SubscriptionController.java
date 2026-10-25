package com.fitnessaddicted.gym.membership.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessaddicted.gym.common.CurrentUser;
import com.fitnessaddicted.gym.membership.service.ExpiringSubscriptionService;
import com.fitnessaddicted.gym.membership.service.SubscriptionService;
import com.fitnessaddicted.gym.payment.domain.PaymentMethod;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class SubscriptionController {

    private static final int MAX_EXPIRY_WINDOW_DAYS = 90;

    private final SubscriptionService subscriptionService;
    private final ExpiringSubscriptionService expiringService;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  ExpiringSubscriptionService expiringService) {
        this.subscriptionService = subscriptionService;
        this.expiringService = expiringService;
    }

    @GetMapping("/members/me/subscriptions")
    @PreAuthorize("hasRole('MEMBER')")
    public List<SubscriptionResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return subscriptionService.listForMember(CurrentUser.id(jwt));
    }

    @GetMapping("/members/{memberId}/subscriptions")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<SubscriptionResponse> forMember(@PathVariable Long memberId) {
        return subscriptionService.listForMember(memberId);
    }

    /** Sells a plan to a member (front desk). */
    @PostMapping("/members/{memberId}/subscriptions")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse purchase(@AuthenticationPrincipal Jwt jwt, @PathVariable Long memberId,
                                         @Valid @RequestBody PurchaseRequest request) {
        PaymentMethod method = request.method() == null ? PaymentMethod.CASH : request.method();
        return subscriptionService.purchase(CurrentUser.id(jwt), memberId, request.planId(),
                method, request.reference());
    }

    /** Memberships ending within the next N days (default 7), with reminder status, for renewal follow-up. */
    @GetMapping("/subscriptions/expiring")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<ExpiringSubscriptionResponse> expiring(@RequestParam(defaultValue = "7") int days) {
        int window = Math.min(Math.max(days, 1), MAX_EXPIRY_WINDOW_DAYS);
        return expiringService.expiringWithin(window);
    }

    /** Members may freeze their own membership; staff may freeze anyone's. */
    @PostMapping("/subscriptions/{id}/freeze")
    @PreAuthorize("hasAnyRole('MEMBER','STAFF','ADMIN')")
    public SubscriptionResponse freeze(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                       @Valid @RequestBody FreezeRequest request) {
        return subscriptionService.freeze(CurrentUser.id(jwt), CurrentUser.isStaff(jwt), id, request.days());
    }

    @PostMapping("/subscriptions/{id}/unfreeze")
    @PreAuthorize("hasAnyRole('MEMBER','STAFF','ADMIN')")
    public SubscriptionResponse unfreeze(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return subscriptionService.unfreeze(CurrentUser.id(jwt), CurrentUser.isStaff(jwt), id);
    }

    @PostMapping("/subscriptions/{id}/cancel")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public SubscriptionResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return subscriptionService.cancel(CurrentUser.id(jwt), id);
    }
}