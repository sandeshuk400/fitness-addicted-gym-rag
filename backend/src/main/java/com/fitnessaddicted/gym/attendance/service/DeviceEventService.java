package com.fitnessaddicted.gym.attendance.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.common.AppTime;
import com.fitnessaddicted.gym.membership.service.SubscriptionService;

/**
 * Logs every door scan and marks attendance once per member per day. The device opens the door
 * by itself; this service only records what happened.
 */
@Service
public class DeviceEventService {

    private final JdbcTemplate jdbc;
    private final SubscriptionService subscriptions;

    public DeviceEventService(JdbcTemplate jdbc, SubscriptionService subscriptions) {
        this.jdbc = jdbc;
        this.subscriptions = subscriptions;
    }

    @Transactional
    public String record(String deviceSerial, int deviceUserId, Instant scannedAt) {
        List<Long> ids = jdbc.queryForList(
                "select user_id from member_profile where device_user_id = ?", Long.class, deviceUserId);
        Long memberId = ids.isEmpty() ? null : ids.get(0);

        String result;
        if (memberId == null) {
            result = "UNKNOWN_USER";
        } else if (!subscriptions.hasActiveMembership(memberId)) {
            result = "DENIED_NO_MEMBERSHIP";
        } else {
            int created = jdbc.update(
                    "insert into attendance (member_id, check_in, source, visit_date) values (?, ?, 'FINGERPRINT', ?) "
                            + "on conflict (member_id, visit_date) do nothing",
                    memberId, Timestamp.from(scannedAt),
                    java.sql.Date.valueOf(scannedAt.atZone(AppTime.GYM_ZONE).toLocalDate()));
            result = created == 1 ? "ATTENDANCE_MARKED" : "RE_ENTRY";
        }

        int logged = jdbc.update(
                "insert into door_event (device_serial, device_user_id, member_id, scanned_at, result) "
                        + "values (?, ?, ?, ?, ?) on conflict do nothing",
                deviceSerial, deviceUserId, memberId, Timestamp.from(scannedAt), result);
        return logged == 1 ? result : "DUPLICATE";
    }
}