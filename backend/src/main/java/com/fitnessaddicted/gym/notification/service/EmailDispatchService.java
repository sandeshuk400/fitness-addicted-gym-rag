package com.fitnessaddicted.gym.notification.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fitnessaddicted.gym.notification.domain.Notification;
import com.fitnessaddicted.gym.notification.repository.NotificationRepository;

/**
 * Sends the EMAIL notifications that are waiting. Each row is saved right after its own attempt, so one bad
 * address never blocks the others. A failed row stays PENDING and is retried on the next run, until
 * MAX_ATTEMPTS is reached; then it is FAILED and left alone. If the app dies between sending and saving,
 * that one email may be sent twice on restart, which is acceptable for notices like these.
 */
@Service
public class EmailDispatchService {

    public static final int MAX_ATTEMPTS = 5;

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchService.class);

    private final NotificationRepository notifications;
    private final EmailSender sender;
    private final JdbcTemplate jdbc;

    public EmailDispatchService(NotificationRepository notifications, EmailSender sender, JdbcTemplate jdbc) {
        this.notifications = notifications;
        this.sender = sender;
        this.jdbc = jdbc;
    }

    /** @return how many emails were sent in this run */
    public int sendPending() {
        int sent = 0;
        for (Notification n : notifications.findTop50ByChannelAndStatusOrderByIdAsc(
                Notification.CHANNEL_EMAIL, Notification.STATUS_PENDING)) {
            try {
                List<String> addresses = jdbc.queryForList(
                        "select email from app_user where id = ?", String.class, n.getUserId());
                if (addresses.isEmpty()) {
                    n.markFailed(); // the account is gone, so there is nobody to send to
                } else {
                    sender.send(addresses.get(0), n.getTitle(), n.getMessage());
                    n.markSent();
                    sent++;
                }
            } catch (RuntimeException e) {
                n.recordFailedAttempt(MAX_ATTEMPTS);
                // Class name only: SMTP messages can contain the recipient's address.
                log.warn("Email for notification {} failed (attempt {} of {}): {}",
                        n.getId(), n.getAttempts(), MAX_ATTEMPTS, e.getClass().getSimpleName());
                log.debug("Email failure details", e);
            }
            notifications.save(n);
        }
        return sent;
    }
}