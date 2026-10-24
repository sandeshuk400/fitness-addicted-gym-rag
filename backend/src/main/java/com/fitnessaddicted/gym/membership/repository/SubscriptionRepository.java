package com.fitnessaddicted.gym.membership.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessaddicted.gym.membership.domain.Subscription;
import com.fitnessaddicted.gym.membership.domain.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByMemberIdOrderByStartDateDesc(Long memberId);

    List<Subscription> findByMemberIdAndStatusOrderByStartDateAsc(Long memberId, SubscriptionStatus status);

    List<Subscription> findByMemberIdAndStatusAndStartDateAfter(Long memberId, SubscriptionStatus status,
                                                                LocalDate after);

    List<Subscription> findByStatusAndEndDateBetweenOrderByEndDateAsc(SubscriptionStatus status,
                                                                      LocalDate from, LocalDate to);

    /** Frozen memberships whose freeze has run out (same date rule as endFreezes). */
    List<Subscription> findByStatusAndFrozenUntilLessThanEqual(SubscriptionStatus status, LocalDate date);

    boolean existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long memberId, SubscriptionStatus status, LocalDate startOnOrBefore, LocalDate endOnOrAfter);

    /** Latest end date among the member's current and queued subscriptions (null if none). */
    @Query("select max(s.endDate) from Subscription s "
            + "where s.memberId = :memberId and s.status in :statuses")
    LocalDate findLatestEndDate(@Param("memberId") Long memberId,
                                @Param("statuses") Collection<SubscriptionStatus> statuses);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Subscription s set s.status = :to, s.frozenUntil = null "
            + "where s.status = :from and s.frozenUntil <= :today")
    int endFreezes(@Param("from") SubscriptionStatus from, @Param("to") SubscriptionStatus to,
                   @Param("today") LocalDate today);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Subscription s set s.status = :to where s.status = :from and s.startDate <= :today")
    int startQueued(@Param("from") SubscriptionStatus from, @Param("to") SubscriptionStatus to,
                    @Param("today") LocalDate today);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Subscription s set s.status = :to where s.status = :from and s.endDate < :today")
    int expireEnded(@Param("from") SubscriptionStatus from, @Param("to") SubscriptionStatus to,
                    @Param("today") LocalDate today);
}