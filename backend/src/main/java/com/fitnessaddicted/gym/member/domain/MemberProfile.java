package com.fitnessaddicted.gym.member.domain;

import java.time.LocalDate;

import com.fitnessaddicted.gym.common.AppTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The primary key is the app_user id: one profile per member account. */
@Entity
@Table(name = "member_profile")
public class MemberProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(name = "emergency_contact", length = 150)
    private String emergencyContact;

    /** Sensitive. Never sent to the LLM; readable only by the member and admins (audited). */
    @Column(name = "health_notes")
    private String healthNotes;

    @Column(name = "joined_on", nullable = false)
    private LocalDate joinedOn = AppTime.today();

    protected MemberProfile() {
        // for JPA
    }

    public MemberProfile(Long userId, String fullName, String phone) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
    }

    public void update(String fullName, String phone, LocalDate dateOfBirth, String gender,
                       String emergencyContact, String healthNotes) {
        this.fullName = fullName;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.emergencyContact = emergencyContact;
        this.healthNotes = healthNotes;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public String getHealthNotes() {
        return healthNotes;
    }

    public LocalDate getJoinedOn() {
        return joinedOn;
    }
}
