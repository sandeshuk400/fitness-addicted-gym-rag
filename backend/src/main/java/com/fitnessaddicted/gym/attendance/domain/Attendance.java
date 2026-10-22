package com.fitnessaddicted.gym.attendance.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One gym visit. checkOut stays null while the member is inside. */
@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "check_in", nullable = false)
    private Instant checkIn = Instant.now();

    @Column(name = "check_out")
    private Instant checkOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceSource source;

    protected Attendance() {
        // for JPA
    }

    public Attendance(Long memberId, AttendanceSource source) {
        this.memberId = memberId;
        this.source = source;
    }

    public void checkOut(Instant when) {
        this.checkOut = when;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Instant getCheckIn() {
        return checkIn;
    }

    public Instant getCheckOut() {
        return checkOut;
    }

    public AttendanceSource getSource() {
        return source;
    }
}
