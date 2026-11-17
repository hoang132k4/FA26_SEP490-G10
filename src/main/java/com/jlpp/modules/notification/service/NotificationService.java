package com.jlpp.modules.notification.service;

import com.jlpp.modules.notification.entity.Notification;

import java.util.List;

public interface NotificationService {

    List<Notification> getRecentNotifications(Long userId);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);

    void resetUnreadForDemo();

    void initSampleNotificationsIfEmpty();
}
