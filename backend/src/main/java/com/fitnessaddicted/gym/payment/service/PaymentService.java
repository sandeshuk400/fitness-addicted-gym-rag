package com.fitnessaddicted.gym.payment.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
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
 *
 * <p>Rules: a member pays a one-time joining fee (separate payment and receipt, never refundable) with
 * their first plan. Plan payments can be refunded only within a set number of days of purchase.
 * At most N plan sales are accepted per gym day (Indian time).
 */
@Service
public class PaymentService {

    private static final String KIND_PLAN = "PLAN";
    private static final String KIND_JOINING_FEE = "JOINING_FEE";

    private final PaymentRepository payments;
    private final SubscriptionRepository subscriptions;
    private final SubscriptionQueue queue;
    private final AuditService audit;
    private final JdbcTemplate jdbc;
    private final PaymentNotificationService paymentNotices;
    private final BigDecimal joiningFee;
    private final int refundWindowDays;
    private final int maxSalesPerDay;

    public PaymentService(PaymentRepository payments, SubscriptionRepository subscriptions,
                          SubscriptionQueue queue, AuditService audit, JdbcTemplate jdbc,
                          PaymentNotificationService paymentNotices,
                          @Value("${app.membership.joining-fee:500}") BigDecimal joiningFee,
                          @Value("${app.membership.refund-window-days:7}") int refundWindowDays,
                          @Value("${app.membership.max-sales-per-day:50}") int maxSalesPerDay) {
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.queue = queue;
        this.audit = audit;
        this.jdbc = jdbc;
        this.paymentNotices = paymentNotices;
        this.joiningFee = joiningFee;
        this.refundWindowDays = refundWindowDays;
        this.maxSalesPerDay = maxSalesPerDay;
    }

    /**
     * Amount is the plan price as of now, never supplied by the client. If the member has never paid the
     * joining fee, it is recorded as a second payment with its own receipt.
     */
    @Transactional
    public Payment recordSale(Long actorId, Subscription subscription, MembershipPlan plan,
                              PaymentMethod method, String reference) {
        if (salesToday() >= maxSalesPerDay) {
            throw ApiException.conflict("Daily limit reached: at most " + maxSalesPerDay
                    + " memberships can be sold per day");
        }
        String ref = (reference == null || reference.isBlank()) ? null : reference.trim();

        boolean chargeFee = !hasPaidJoiningFee(subscription.getMemberId());
        if (chargeFee) {
            String feeReceipt = nextReceiptNumber();
            Payment fee = Payment.paid(subscription.getId(), joiningFee, method, ref,
                    feeReceipt, actorId, Instant.now());
            fee.markJoiningFee();
            Payment savedFee = payments.save(fee);
            audit.record(actorId, "JOINING_FEE_RECORDED", "Payment", String.valueOf(savedFee.getId()),
                    Map.of("subscriptionId", subscription.getId(), "amount", savedFee.getAmount().toPlainString(),
                            "receipt", feeReceipt));
            paymentNotices.sendReceipt(subscription.getMemberId(), savedFee.getId(), "Joining fee",
                    savedFee.getCurrency() + " " + savedFee.getAmount().toPlainString(), feeReceipt);
        }

        String receipt = nextReceiptNumber();
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
     * the member's queued plans up so there is no gap. The joining fee is never refundable, and plan
     * payments only within the refund window.
     */
    @Transactional
    public PaymentResponse refund(Long actorId, Long paymentId, String reason) {
        Payment p = payments.findForUpdate(paymentId)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        if (p.getStatus() != PaymentStatus.PAID) {
            throw ApiException.conflict("Only a paid payment can be refunded");
        }
        if (KIND_JOINING_FEE.equals(p.getKind())) {
            throw ApiException.badRequest("The joining fee is non-refundable");
        }
        if (p.getPaidAt() != null) {
            LocalDate paidOn = p.getPaidAt().atZone(AppTime.GYM_ZONE).toLocalDate();
            if (ChronoUnit.DAYS.between(paidOn, AppTime.today()) > refundWindowDays) {
                throw ApiException.badRequest("Refunds are only possible within " + refundWindowDays
                        + " days of purchase");
            }
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

    /** Plan sales recorded so far today (Indian calendar day). */
    private long salesToday() {
        LocalDate today = AppTime.today();
        Timestamp from = Timestamp.from(today.atStartOfDay(AppTime.GYM_ZONE).toInstant());
        Timestamp to = Timestamp.from(today.plusDays(1).atStartOfDay(AppTime.GYM_ZONE).toInstant());
        Long n = jdbc.queryForObject(
                "select count(*) from payment where kind = ? and created_at >= ? and created_at < ?",
                Long.class, KIND_PLAN, from, to);
        return n == null ? 0 : n;
    }

    private boolean hasPaidJoiningFee(Long memberId) {
        Long n = jdbc.queryForObject(
                "select count(*) from payment p join subscription s on s.id = p.subscription_id "
                        + "where s.member_id = ? and p.kind = ?",
                Long.class, memberId, KIND_JOINING_FEE);
        return n != null && n > 0;
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