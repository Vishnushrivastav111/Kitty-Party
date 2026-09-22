package com.microvault.dao;

import com.microvault.model.Notification;

import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "notifications" table. Only data access is
 * described here, no business rules.
 */
public interface NotificationDAO {

    Notification create(Notification notification);

    Notification findById(UUID id);

    List<Notification> findAll();

    List<Notification> findByUserId(UUID userId);

    List<Notification> findUnreadByUserId(UUID userId);

    boolean markAsRead(UUID id);

    boolean markAllAsRead(UUID userId);

    boolean update(Notification notification);

    boolean softDelete(UUID id);
}
