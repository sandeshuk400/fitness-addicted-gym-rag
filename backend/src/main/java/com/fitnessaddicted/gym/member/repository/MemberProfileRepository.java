package com.fitnessaddicted.gym.member.repository;

import com.fitnessaddicted.gym.member.domain.MemberProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    Page<MemberProfile> findByFullNameContainingIgnoreCaseOrPhoneContaining(String name, String phone, Pageable pageable);
}
