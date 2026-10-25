package com.fitnessaddicted.gym.payment.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.common.AppTime;
import com.fitnessaddicted.gym.common.AuditService;
import com.fitnessaddicted.gym.membership.domain.MembershipPlan;
import com.fitnessaddicted.gym.membership.domain.Subscription;
import com.fitnessaddicted.gym.membership.domain.SubscriptionStatus;
import com.fitnessaddicted.gym.membership.repository.SubscriptionRepository;
import com.fitnessaddicted.gym.membership.service.SubscriptionQueue;
import com.fitnessaddicted.gym.notification.service.PaymentNotificationService;
import com.fitnessaddicted.gym.payment.api.PaymentResponse;
import com.fitnessaddicted.gym.payment.domain.Payment;
import com.fitnessaddicted.gym.payment.domain.PaymentMethod;
import com.fitnessaddicted.gym.payment.domain.PaymentStatus;
import com.fitnessaddicted.gym.payment.repository.PaymentRepository;

/**
 * Manual (front-desk) payments. A sale records its payment in the caller's transaction, so a subscription
 * never exists without one. This service reads subscriptions only through the repository, so
 * SubscriptionService can depend on it without a cycle.
 */
@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final SubscriptionRepository subscriptions;
    private final SubscriptionQueue queue;
    private final AuditService audit;
    private final JdbcTemplate jdbc;
    private final PaymentNotificationService paymentNotices;

    public PaymentService(PaymentRepository payments, SubscriptionRepository subscriptions,
                          SubscriptionQueue queue, AuditService audit, JdbcTemplate jdbc,
                          PaymentNotificationService paymentNotices) {
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.queue = queue;
        this.audit = audit;
        this.jdbc = jdbc;
        this.paymentNotices = paymentNotices;
    }

    /** Amount is the plan price as of now, never supplied by the client. */
    @Transactional
    public Payment recordSale(Long actorId, Subscription subscription, MembershipPlan plan,
                              PaymentMethod method, String reference) {
        String receipt = nextReceiptNumber();
        String ref = (reference == null || reference.isBlank()) ? null : reference.trim();
        Payment saved = payments.save(Payment.paid(subscription.getId(), plan.getPrice(), method, ref,
                receipt, actorId, Instant.now()));
        audit.record(actorId, "PAYMENT_RECORDED", "Payment", String.valueOf(saved.getId()),
                Map.of("subscriptionId", subscription.getId(), "amount", saved.getAmount().toPlainString(),
                        "method", method.name(), "receipt", receipt));
        paymentNotices.sendReceipt(subscription.getMemberId(), saved.getId(), plan.getName(),
                saved.getCurrency() + " " + saved.getAmount().toPlainString(), receipt);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listForMember(Long memberId) {
        return toResponses(payments.findByMemberId(memberId));
    }

    /**
     * Admin only (controller enforces it). Also cancels the subscription the payment bought and moves
     * the member's queued plans up so there is no gap.
     */
    @Transactional
    public PaymentResponse refund(Long actorId, Long paymentId, String reason) {
        Payment p = payments.findForUpdate(paymentId)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        if (p.getStatus() != PaymentStatus.PAID) {
            throw ApiException.conflict("Only a paid payment can be refunded");
        }
        p.markRefunded(Instant.now(), actorId, reason.trim());
        Subscription sub = subscriptions.findById(p.getSubscriptionId()).orElse(null);
        if (sub != null) {
            if (sub.getStatus() != SubscriptionStatus.CANCELLED && sub.getStatus() != SubscriptionStatus.EXPIRED) {
                sub.cancel();
                queue.compact(sub.getMemberId());
            }
            paymentNotices.sendRefund(sub.getMemberId(), p.getId(),
                    p.getCurrency() + " " + p.getAmount().toPlainString(), p.getReceiptNumber());
        }
        audit.record(actorId, "PAYMENT_REFUNDED", "Payment", String.valueOf(paymentId),
                Map.of("receipt", String.valueOf(p.getReceiptNumber()), "reason", reason.trim()));
        return toResponses(List.of(p)).get(0);
    }

    private String nextReceiptNumber() {
        Long n = jdbc.queryForObject("select nextval('payment_receipt_seq')", Long.class);
        return String.format("RCP-%d-%06d", AppTime.today().getYear(), n);
    }

    private List<PaymentResponse> toResponses(List<Payment> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> memberBySubscription = subscriptions.findAllById(
                        list.stream().map(Payment::getSubscriptionId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Subscription::getId, Subscription::getMemberId));
        return list.stream()
                .map(p -> PaymentResponse.from(p, memberBySubscription.get(p.getSubscriptionId())))
                .toList();
    }
}