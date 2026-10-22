package com.fitnessaddicted.gym.membership.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps subscription statuses current. Runs shortly after midnight gym time, and once at startup so a
 * server that was down overnight catches up. Attendance also checks dates directly, so a few minutes
 * of lag never lets an expired member in.
 */
@Component
public class MembershipScheduler {

    private static final Logger log = LoggerFactory.getLogger(MembershipScheduler.class);

    private final SubscriptionService subscriptions;

    public MembershipScheduler(SubscriptionService subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Kolkata")
    public void nightly() {
        refresh("nightly");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        refresh("startup");
    }

    private void refresh(String trigger) {
        try {
            int changed = subscriptions.refreshStatuses();
            log.info("Subscription status refresh ({}): {} changed", trigger, changed);
        } catch (RuntimeException e) {
            log.error("Subscription status refresh ({}) failed", trigger, e);
        }
    }
}
