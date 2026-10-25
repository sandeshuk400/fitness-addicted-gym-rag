package com.fitnessaddicted.gym.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.notification.domain.Notification;
import com.fitnessaddicted.gym.notification.repository.NotificationRepository;

/**
 * Creates in-app payment notices (receipt on sale, notice on refund). Takes plain values, not payment
 * classes, so the notification package does not depend on the payment package. Called inside the
 * payment's transaction.
 */
@Service
public class PaymentNotificationService {

    private final NotificationRepository notifications;

    public PaymentNotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public void sendReceipt(Long memberId, Long paymentId, String planName, String amountText,
                            String receiptNumber) {
        notifications.save(Notification.paymentReceipt(memberId, paymentId, "Payment received",
                "We received " + amountText + " for your " + planName + " membership. "
                        + "Receipt number: " + receiptNumber + "."));
    }

    @Transactional
    public void sendRefund(Long memberId, Long paymentId, String amountText, String receiptNumber) {
        notifications.save(Notification.paymentRefund(memberId, paymentId, "Payment refunded",
                "Your payment of " + amountText + " (receipt " + receiptNumber + ") has been refunded."));
    }
}