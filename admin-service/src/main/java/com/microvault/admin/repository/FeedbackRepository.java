package com.microvault.admin.repository;

import com.microvault.admin.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    List<Feedback> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);
    List<Feedback> findByDeletedFalseOrderByCreatedAtDesc();
    Optional<Feedback> findByIdAndDeletedFalse(UUID id);
}
