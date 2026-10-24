package com.fitnessaddicted.gym.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.notification.domain.Notification;
import com.fitnessaddicted.gym.notification.repository.NotificationRepository;

/**
 * Creates the in-app receipt notice for a sale. Takes plain values, not payment classes, so the
 * notification package does not depend on the payment package. Called inside the sale's transaction.
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
}