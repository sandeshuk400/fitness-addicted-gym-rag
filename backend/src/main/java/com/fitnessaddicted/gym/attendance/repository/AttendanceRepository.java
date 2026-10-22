package com.fitnessaddicted.gym.attendance.repository;

import java.time.Instant;
import java.util.Optional;

import com.fitnessaddicted.gym.attendance.domain.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findFirstByMemberIdAndCheckOutIsNullAndCheckInAfterOrderByCheckInDesc(
            Long memberId, Instant after);

    long countByCheckOutIsNullAndCheckInAfter(Instant after);

    Page<Attendance> findByMemberIdOrderByCheckInDesc(Long memberId, Pageable pageable);
}
