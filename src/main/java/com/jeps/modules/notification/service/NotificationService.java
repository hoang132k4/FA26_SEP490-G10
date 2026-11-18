package com.jeps.modules.notification.service;

import com.jeps.modules.notification.entity.Notification;

import java.util.List;

public interface NotificationService {

    List<Notification> getRecentNotifications(Long userId);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);

    void resetUnreadForDemo();

    void initSampleNotificationsIfEmpty();
}
