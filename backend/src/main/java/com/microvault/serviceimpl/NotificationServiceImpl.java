package com.microvault.serviceimpl;

import com.microvault.dao.NotificationDAO;
import com.microvault.dto.NotificationDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.Notification;
import com.microvault.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for notifications. The DAO is received through the
 * constructor, so this class is bound to the NotificationDAO interface and
 * not to a concrete class.
 */
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDAO notificationDAO;

    public NotificationServiceImpl(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    @Override
    public NotificationDTO createNotification(Notification notification) {

        validateNotification(notification);

        if (notification.getType() == null || notification.getType().trim().isEmpty()) {
            notification.setType("info");
        }
        if (notification.getIsRead() == null) {
            notification.setIsRead(Boolean.FALSE);
        }

        Notification savedNotification = notificationDAO.create(notification);
        return toDTO(savedNotification);
    }

    @Override
    public NotificationDTO getNotificationById(UUID id) {
        if (id == null) {
            throw new ValidationException("Notification id is required");
        }
        Notification notification = notificationDAO.findById(id);
        if (notification == null) {
            throw new ValidationException("No active notification found with id " + id);
        }
        return toDTO(notification);
    }

    @Override
    public List<NotificationDTO> getAllNotifications() {
        return toDTOList(notificationDAO.findAll());
    }

    @Override
    public List<NotificationDTO> getNotificationsByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toDTOList(notificationDAO.findByUserId(userId));
    }

    @Override
    public List<NotificationDTO> getUnreadNotifications(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toDTOList(notificationDAO.findUnreadByUserId(userId));
    }

    @Override
    public boolean markAsRead(UUID id) {
        if (id == null) {
            throw new ValidationException("Notification id is required");
        }
        if (notificationDAO.findById(id) == null) {
            throw new ValidationException("No active notification found with id " + id);
        }
        return notificationDAO.markAsRead(id);
    }

    @Override
    public boolean markAllAsRead(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return notificationDAO.markAllAsRead(userId);
    }

    @Override
    public boolean updateNotification(Notification notification) {

        if (notification == null || notification.getId() == null) {
            throw new ValidationException("Notification id is required for an update");
        }
        validateNotification(notification);

        Notification existingNotification = notificationDAO.findById(notification.getId());
        if (existingNotification == null) {
            throw new ValidationException("No active notification found with id " + notification.getId());
        }

        if (notification.getType() == null || notification.getType().trim().isEmpty()) {
            notification.setType("info");
        }
        if (notification.getIsRead() == null) {
            notification.setIsRead(Boolean.FALSE);
        }

        return notificationDAO.update(notification);
    }

    @Override
    public boolean softDeleteNotification(UUID id) {
        if (id == null) {
            throw new ValidationException("Notification id is required");
        }
        if (notificationDAO.findById(id) == null) {
            throw new ValidationException("No active notification found with id " + id);
        }
        return notificationDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateNotification(Notification notification) {

        if (notification == null) {
            throw new ValidationException("Notification is required");
        }
        if (notification.getUserId() == null) {
            throw new ValidationException("User id is required");
        }
        if (notification.getTitle() == null || notification.getTitle().trim().isEmpty()) {
            throw new ValidationException("Title is required");
        }
        if (notification.getMessage() == null || notification.getMessage().trim().isEmpty()) {
            throw new ValidationException("Message is required");
        }
        if (notification.getType() != null && !notification.getType().trim().isEmpty()
                && !notification.getType().matches("info|success|warning|danger")) {
            throw new ValidationException("Type must be info, success, warning or danger");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private NotificationDTO toDTO(Notification notification) {

        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setId(notification.getId());
        notificationDTO.setUserId(notification.getUserId());
        notificationDTO.setTitle(notification.getTitle());
        notificationDTO.setMessage(notification.getMessage());
        notificationDTO.setType(notification.getType());
        notificationDTO.setIsRead(notification.getIsRead());
        notificationDTO.setCreatedAt(notification.getCreatedAt());
        return notificationDTO;
    }

    private List<NotificationDTO> toDTOList(List<Notification> notifications) {
        List<NotificationDTO> notificationDTOs = new ArrayList<>();
        for (Notification notification : notifications) {
            notificationDTOs.add(toDTO(notification));
        }
        return notificationDTOs;
    }
}
