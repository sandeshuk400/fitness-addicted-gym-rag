package com.fitnessaddicted.gym.payment.api;

import java.math.BigDecimal;
import java.time.Instant;

import com.fitnessaddicted.gym.payment.domain.Payment;
import com.fitnessaddicted.gym.payment.domain.PaymentMethod;
import com.fitnessaddicted.gym.payment.domain.PaymentStatus;

public record PaymentResponse(Long id, Long subscriptionId, Long memberId, BigDecimal amount, String currency,
                              PaymentStatus status, PaymentMethod method, String reference, String receiptNumber,
                              Instant paidAt, Instant refundedAt, String refundReason) {

    public static PaymentResponse from(Payment p, Long memberId) {
        return new PaymentResponse(p.getId(), p.getSubscriptionId(), memberId, p.getAmount(), p.getCurrency(),
                p.getStatus(), p.getMethod(), p.getReference(), p.getReceiptNumber(), p.getPaidAt(),
                p.getRefundedAt(), p.getRefundReason());
    }
}