package com.fitnessaddicted.gym.notification.service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the email dispatch every 5 minutes, but only when app.notifications.email.enabled is true. */
@Component
public class EmailDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchJob.class);

    private final EmailDispatchService dispatch;
    private final boolean enabled;

    public EmailDispatchJob(EmailDispatchService dispatch,
                            @Value("${app.notifications.email.enabled:false}") boolean enabled) {
        this.dispatch = dispatch;
        this.enabled = enabled;
    }

    @Scheduled(initialDelay = 1, fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void run() {
        if (!enabled) {
            return;
        }
        try {
            int sent = dispatch.sendPending();
            if (sent > 0) {
                log.info("Emails sent: {}", sent);
            }
        } catch (RuntimeException e) {
            log.error("Email dispatch failed", e);
        }
    }
}