package com.fitnessaddicted.gym.member.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fitnessaddicted.gym.member.domain.MemberProfile;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    Page<MemberProfile> findByFullNameContainingIgnoreCaseOrPhoneContaining(String name, String phone, Pageable pageable);

    Optional<MemberProfile> findByDeviceUserId(Integer deviceUserId);
}