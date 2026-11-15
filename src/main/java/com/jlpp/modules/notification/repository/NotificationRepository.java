package com.jlpp.modules.notification.repository;

import com.jlpp.modules.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(value = "SELECT * FROM notifications WHERE (user_id = :userId OR user_id IS NULL) ORDER BY created_at DESC LIMIT 15", nativeQuery = true)
    List<Notification> findAllRecentNotificationsByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM notifications ORDER BY created_at DESC LIMIT 15", nativeQuery = true)
    List<Notification> findAllRecentNotifications();

    @Query(value = "SELECT COUNT(*) FROM notifications WHERE is_read = false AND (user_id = :userId OR user_id IS NULL)", nativeQuery = true)
    long countUnreadByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) FROM notifications WHERE is_read = false", nativeQuery = true)
    long countAllUnread();

    @Modifying
    @Query(value = "UPDATE notifications SET is_read = true WHERE (user_id = :userId OR user_id IS NULL)", nativeQuery = true)
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE notifications SET is_read = true", nativeQuery = true)
    void markAllAsRead();
}
