package com.fitnessaddicted.gym.member.repository;

import java.util.Optional;

import com.fitnessaddicted.gym.member.domain.ConsentPurpose;
import com.fitnessaddicted.gym.member.domain.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    Optional<ConsentRecord> findFirstByUserIdAndPurposeOrderByRecordedAtDescIdDesc(Long userId, ConsentPurpose purpose);
}
