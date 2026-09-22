package com.microvault.service;

import com.microvault.dto.NotificationDTO;
import com.microvault.model.Notification;

import java.util.List;
import java.util.UUID;

/**
 * Business operations for notifications: validation, defaults and the read
 * flag shown in the notification bell.
 */
public interface NotificationService {

    NotificationDTO createNotification(Notification notification);

    NotificationDTO getNotificationById(UUID id);

    List<NotificationDTO> getAllNotifications();

    List<NotificationDTO> getNotificationsByUserId(UUID userId);

    List<NotificationDTO> getUnreadNotifications(UUID userId);

    boolean markAsRead(UUID id);

    boolean markAllAsRead(UUID userId);

    boolean updateNotification(Notification notification);

    boolean softDeleteNotification(UUID id);
}
