package com.fitnessaddicted.gym.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessaddicted.gym.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(Long userId, String channel);

    long countByUserIdAndChannelAndReadAtIsNull(Long userId, String channel);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /** Explicit query: a derived name would read "DaysBefore" as the keyword "Before". */
    @Query("select count(n) > 0 from Notification n "
            + "where n.subscriptionId = :subscriptionId and n.daysBefore = :daysBefore "
            + "and n.channel = :channel")
    boolean existsBySubscriptionIdAndDaysBeforeAndChannel(@Param("subscriptionId") Long subscriptionId,
                                                          @Param("daysBefore") int daysBefore,
                                                          @Param("channel") String channel);
}