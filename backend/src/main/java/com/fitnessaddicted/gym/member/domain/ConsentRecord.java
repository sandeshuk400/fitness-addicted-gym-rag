package com.fitnessaddicted.gym.member.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Append-only: every grant or withdrawal is a new row, so the full consent history is kept. */
@Entity
@Table(name = "consent_record")
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsentPurpose purpose;

    @Column(nullable = false)
    private boolean granted;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    protected ConsentRecord() {
        // for JPA
    }

    public ConsentRecord(Long userId, ConsentPurpose purpose, boolean granted) {
        this.userId = userId;
        this.purpose = purpose;
        this.granted = granted;
    }

    public ConsentPurpose getPurpose() {
        return purpose;
    }

    public boolean isGranted() {
        return granted;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
