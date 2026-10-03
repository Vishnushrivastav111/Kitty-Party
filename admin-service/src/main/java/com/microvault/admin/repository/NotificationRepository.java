package com.microvault.admin.repository;

import com.microvault.admin.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);
    Optional<Notification> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
