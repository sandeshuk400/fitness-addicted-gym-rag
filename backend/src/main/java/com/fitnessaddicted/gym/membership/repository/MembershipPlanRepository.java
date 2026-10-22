package com.fitnessaddicted.gym.membership.repository;

import java.util.List;

import com.fitnessaddicted.gym.membership.domain.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    List<MembershipPlan> findByActiveTrueOrderByPriceAsc();

    List<MembershipPlan> findAllByOrderByPriceAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
