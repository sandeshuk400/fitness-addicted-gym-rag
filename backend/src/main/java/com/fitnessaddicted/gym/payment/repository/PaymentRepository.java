package com.fitnessaddicted.gym.payment.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessaddicted.gym.payment.domain.Payment;
import com.fitnessaddicted.gym.payment.domain.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("select p from Payment p where p.subscriptionId in "
            + "(select s.id from Subscription s where s.memberId = :memberId) order by p.createdAt desc, p.id desc")
    List<Payment> findByMemberId(@Param("memberId") Long memberId);

    /** Row lock so two simultaneous refunds cannot both succeed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findForUpdate(@Param("id") Long id);

    /** Total of payments with the given status paid in [from, to). Null when there are none. */
    @Query("select sum(p.amount) from Payment p "
            + "where p.status = :status and p.paidAt >= :from and p.paidAt < :to")
    BigDecimal sumAmountBetween(@Param("status") PaymentStatus status,
                                @Param("from") Instant from, @Param("to") Instant to);
}