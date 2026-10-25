package com.fitnessaddicted.gym.notification.repository;

import java.util.Collection;
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

    /** Rows of [subscriptionId, smallest daysBefore sent] for the given subscriptions. */
    @Query("select n.subscriptionId, min(n.daysBefore) from Notification n "
            + "where n.subscriptionId in :ids and n.type = :type and n.channel = :channel "
            + "group by n.subscriptionId")
    List<Object[]> findLastReminderMarks(@Param("ids") Collection<Long> ids, @Param("type") String type,
                                         @Param("channel") String channel);
}