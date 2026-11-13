package com.fitnessaddicted.gym.attendance.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fitnessaddicted.gym.attendance.domain.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findFirstByMemberIdAndCheckOutIsNullAndCheckInAfterOrderByCheckInDesc(
            Long memberId, Instant after);

    Optional<Attendance> findByMemberIdAndVisitDate(Long memberId, LocalDate visitDate);

    long countByCheckOutIsNullAndCheckInAfter(Instant after);
    long countByCheckInGreaterThanEqual(Instant from);

    Page<Attendance> findByMemberIdOrderByCheckInDesc(Long memberId, Pageable pageable);
}
