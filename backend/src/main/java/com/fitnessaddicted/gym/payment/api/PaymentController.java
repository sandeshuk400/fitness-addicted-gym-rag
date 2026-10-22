package com.fitnessaddicted.gym.payment.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessaddicted.gym.common.CurrentUser;
import com.fitnessaddicted.gym.payment.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/members/me/payments")
    @PreAuthorize("hasRole('MEMBER')")
    public List<PaymentResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return paymentService.listForMember(CurrentUser.id(jwt));
    }

    @GetMapping("/members/{memberId}/payments")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<PaymentResponse> forMember(@PathVariable Long memberId) {
        return paymentService.listForMember(memberId);
    }

    @PostMapping("/payments/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse refund(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody RefundRequest request) {
        return paymentService.refund(CurrentUser.id(jwt), id, request.reason());
    }
}