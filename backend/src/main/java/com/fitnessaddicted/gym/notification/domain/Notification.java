package com.fitnessaddicted.gym.notification.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A message for a user. IN_APP rows are shown in the app at once; EMAIL rows wait as PENDING until the
 * dispatch job sends them.
 */
@Entity
@Table(name = "notification")
public class Notification {

    public static final String CHANNEL_IN_APP = "IN_APP";
    public static final String CHANNEL_EMAIL = "EMAIL";

    public static final String TYPE_SUBSCRIPTION_EXPIRY = "SUBSCRIPTION_EXPIRY";
    public static final String TYPE_FREEZE_ENDED = "FREEZE_ENDED";
    public static final String TYPE_PAYMENT_RECEIPT = "PAYMENT_RECEIPT";
    public static final String TYPE_PAYMENT_REFUND = "PAYMENT_REFUND";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

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

    @Column(nullable = false)
    private int attempts;

    protected Notification() {
        // for JPA
    }

    private static Notification inApp(Long userId, String type, String title, String message) {
        Notification n = new Notification();
        n.userId = userId;
        n.channel = CHANNEL_IN_APP;
        n.type = type;
        n.status = STATUS_SENT; // in-app messages are delivered the moment the row exists
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

    public static Notification paymentRefund(Long userId, Long paymentId, String title, String message) {
        Notification n = inApp(userId, TYPE_PAYMENT_REFUND, title, message);
        n.paymentId = paymentId;
        return n;
    }

    /** An email waiting to be sent by the dispatch job. */
    public static Notification pendingEmail(Long userId, String type, String title, String message) {
        Notification n = new Notification();
        n.userId = userId;
        n.channel = CHANNEL_EMAIL;
        n.type = type;
        n.status = STATUS_PENDING;
        n.title = title;
        n.message = message;
        return n;
    }

    /** A pending email carrying the same text and links as an in-app notice. */
    public static Notification emailCopyOf(Notification source) {
        Notification n = pendingEmail(source.userId, source.type, source.title, source.message);
        n.subscriptionId = source.subscriptionId;
        n.daysBefore = source.daysBefore;
        n.paymentId = source.paymentId;
        return n;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    public void markSent() {
        status = STATUS_SENT;
        sentAt = Instant.now();
    }

    public void markFailed() {
        status = STATUS_FAILED;
    }

    /** Counts a failed send. Once maxAttempts is reached the row is FAILED and is not tried again. */
    public void recordFailedAttempt(int maxAttempts) {
        attempts++;
        if (attempts >= maxAttempts) {
            status = STATUS_FAILED;
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getChannel() { return channel; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Long getSubscriptionId() { return subscriptionId; }
    public Integer getDaysBefore() { return daysBefore; }
    public Long getPaymentId() { return paymentId; }
    public Instant getReadAt() { return readAt; }
    public int getAttempts() { return attempts; }
}