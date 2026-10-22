package com.fitnessaddicted.gym.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Money received for a subscription. The amount is a snapshot of the plan price at the time of sale. */
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod method;

    /** "MANUAL" for front-desk payments; a gateway name later. */
    @Column(length = 30)
    private String gateway;

    /** Free text such as a UPI transaction id. */
    @Column(length = 100)
    private String reference;

    @Column(name = "receipt_number", length = 30)
    private String receiptNumber;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "refunded_by")
    private Long refundedBy;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Payment() {
        // for JPA
    }

    public static Payment paid(Long subscriptionId, BigDecimal amount, PaymentMethod method, String reference,
                               String receiptNumber, Long recordedBy, Instant paidAt) {
        Payment p = new Payment();
        p.subscriptionId = subscriptionId;
        p.amount = amount;
        p.status = PaymentStatus.PAID;
        p.method = method;
        p.gateway = "MANUAL";
        p.reference = reference;
        p.receiptNumber = receiptNumber;
        p.recordedBy = recordedBy;
        p.paidAt = paidAt;
        return p;
    }

    public void markRefunded(Instant when, Long actorId, String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = when;
        this.refundedBy = actorId;
        this.refundReason = reason;
    }

    public Long getId() { return id; }
    public Long getSubscriptionId() { return subscriptionId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public PaymentMethod getMethod() { return method; }
    public String getReference() { return reference; }
    public String getReceiptNumber() { return receiptNumber; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getRefundedAt() { return refundedAt; }
    public String getRefundReason() { return refundReason; }
}