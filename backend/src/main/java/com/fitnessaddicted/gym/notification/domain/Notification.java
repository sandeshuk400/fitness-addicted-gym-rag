package com.fitnessaddicted.gym.notification.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A message for a user. For now only the IN_APP channel is produced; email can reuse the same rows later. */
@Entity
@Table(name = "notification")
public class Notification {

    public static final String CHANNEL_IN_APP = "IN_APP";
    public static final String TYPE_SUBSCRIPTION_EXPIRY = "SUBSCRIPTION_EXPIRY";
    public static final String TYPE_FREEZE_ENDED = "FREEZE_ENDED";
    public static final String TYPE_PAYMENT_RECEIPT = "PAYMENT_RECEIPT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(length = 150)
    private String title;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "days_before")
    private Integer daysBefore;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
        // for JPA
    }

    private static Notification inApp(Long userId, String type, String title, String message) {
        Notification n = new Notification();
        n.userId = userId;
        n.channel = CHANNEL_IN_APP;
        n.type = type;
        n.status = "SENT"; // in-app messages are delivered the moment the row exists
        n.sentAt = Instant.now();
        n.title = title;
        n.message = message;
        return n;
    }

    public static Notification expiryReminder(Long userId, Long subscriptionId, int daysBefore,
                                              String title, String message) {
        Notification n = inApp(userId, TYPE_SUBSCRIPTION_EXPIRY, title, message);
        n.subscriptionId = subscriptionId;
        n.daysBefore = daysBefore;
        return n;
    }

    public static Notification freezeEnded(Long userId, Long subscriptionId, String title, String message) {
        Notification n = inApp(userId, TYPE_FREEZE_ENDED, title, message);
        n.subscriptionId = subscriptionId;
        return n;
    }

    public static Notification paymentReceipt(Long userId, Long paymentId, String title, String message) {
        Notification n = inApp(userId, TYPE_PAYMENT_RECEIPT, title, message);
        n.paymentId = paymentId;
        return n;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Long getSubscriptionId() { return subscriptionId; }
    public Integer getDaysBefore() { return daysBefore; }
    public Long getPaymentId() { return paymentId; }
    public Instant getReadAt() { return readAt; }
}