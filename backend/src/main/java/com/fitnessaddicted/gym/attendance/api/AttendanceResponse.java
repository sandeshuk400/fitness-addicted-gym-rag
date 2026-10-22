package com.fitnessaddicted.gym.attendance.api;

import java.time.Instant;

import com.fitnessaddicted.gym.attendance.domain.Attendance;
import com.fitnessaddicted.gym.attendance.domain.AttendanceSource;

public record AttendanceResponse(Long id, Long memberId, Instant checkIn, Instant checkOut,
                                 AttendanceSource source) {

    public static AttendanceResponse from(Attendance a) {
        return new AttendanceResponse(a.getId(), a.getMemberId(), a.getCheckIn(), a.getCheckOut(), a.getSource());
    }
}
