package com.fitnessaddicted.gym.membership.api;

import com.fitnessaddicted.gym.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** method defaults to CASH when omitted; reference is e.g. a UPI transaction id. */
public record PurchaseRequest(@NotNull Long planId, PaymentMethod method, @Size(max = 100) String reference) {
}