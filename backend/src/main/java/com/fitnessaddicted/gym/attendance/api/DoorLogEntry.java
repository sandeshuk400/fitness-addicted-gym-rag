package com.fitnessaddicted.gym.attendance.api;

import java.time.Instant;

public record DoorLogEntry(Long id, Instant scannedAt, Integer deviceUserId, String deviceSerial,
                           Long memberId, String memberName, String result) {
}