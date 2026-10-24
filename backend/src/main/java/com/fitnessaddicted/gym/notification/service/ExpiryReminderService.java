package com.fitnessaddicted.gym.notification.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.common.AppTime;
import com.fitnessaddicted.gym.membership.domain.Subscription;
import com.fitnessaddicted.gym.membership.domain.SubscriptionStatus;
import com.fitnessaddicted.gym.membership.repository.SubscriptionRepository;
import com.fitnessaddicted.gym.notification.domain.Notification;
import com.fitnessaddicted.gym.notification.repository.NotificationRepository;

/**
 * Creates "your membership is ending" reminders at the 7, 3 and 1 day marks. A subscription gets the
 * smallest mark that still covers its days left, so a server that was down on the exact day catches up
 * on the next run, and each mark is sent at most once per subscription. Members who already have a
 * queued plan behind the running one are not reminded, since they are not about to lose access.
 */
@Service
public class ExpiryReminderService {

    private static final int[] MARKS = {1, 3, 7};

    private final SubscriptionRepository subscriptions;
    private final NotificationRepository notifications;

    public ExpiryReminderService(SubscriptionRepository subscriptions, NotificationRepository notifications) {
        this.subscriptions = subscriptions;
        this.notifications = notifications;
    }

    @Transactional
    public int createReminders() {
        LocalDate today = AppTime.today();
        int created = 0;
        for (Subscription s : subscriptions.findByStatusAndEndDateBetweenOrderByEndDateAsc(
                SubscriptionStatus.ACTIVE, today, today.plusDays(MARKS[MARKS.length - 1]))) {
            if (!subscriptions.findByMemberIdAndStatusAndStartDateAfter(
                    s.getMemberId(), SubscriptionStatus.PENDING, s.getEndDate()).isEmpty()) {
                continue;
            }
            int daysLeft = (int) ChronoUnit.DAYS.between(today, s.getEndDate());
            int mark = markFor(daysLeft);
            if (notifications.existsBySubscriptionIdAndDaysBeforeAndChannel(
                    s.getId(), mark, Notification.CHANNEL_IN_APP)) {
                continue;
            }
            notifications.save(Notification.expiryReminder(s.getMemberId(), s.getId(), mark,
                    "Membership ending soon", messageFor(daysLeft, s.getEndDate())));
            created++;
        }
        return created;
    }

    private static int markFor(int daysLeft) {
        for (int mark : MARKS) {
            if (daysLeft <= mark) {
                return mark;
            }
        }
        return MARKS[MARKS.length - 1];
    }

    private static String messageFor(int daysLeft, LocalDate endDate) {
        String when = daysLeft == 0 ? "today" : daysLeft == 1 ? "tomorrow" : "in " + daysLeft + " days";
        return "Your membership ends " + when + " (" + endDate + "). Visit the front desk to renew.";
    }
}