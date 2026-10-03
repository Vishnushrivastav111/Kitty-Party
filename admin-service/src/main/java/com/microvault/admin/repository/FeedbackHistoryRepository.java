package com.microvault.admin.repository;

import com.microvault.admin.entity.FeedbackHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackHistoryRepository extends JpaRepository<FeedbackHistory, UUID> {
    List<FeedbackHistory> findByFeedbackIdAndDeletedFalseOrderByChangedAtDesc(UUID feedbackId);
}
