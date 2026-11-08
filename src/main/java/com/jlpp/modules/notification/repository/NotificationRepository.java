package com.jlpp.modules.notification.repository;

import com.jlpp.modules.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(value = "SELECT * FROM notifications ORDER BY created_at DESC LIMIT 15", nativeQuery = true)
    List<Notification> findAllRecentNotifications();

    @Query(value = "SELECT COUNT(*) FROM notifications WHERE is_read = false", nativeQuery = true)
    long countAllUnread();

    @Modifying
    @Query(value = "UPDATE notifications SET is_read = true", nativeQuery = true)
    void markAllAsRead();
}
