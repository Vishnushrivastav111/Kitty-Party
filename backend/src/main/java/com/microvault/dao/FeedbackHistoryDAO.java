package com.microvault.dao;

import com.microvault.model.FeedbackHistory;

import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "feedback_history" table. Only data access is
 * described here, no business rules.
 */
public interface FeedbackHistoryDAO {

    FeedbackHistory create(FeedbackHistory feedbackHistory);

    FeedbackHistory findById(UUID id);

    List<FeedbackHistory> findAll();

    List<FeedbackHistory> findByFeedbackId(UUID feedbackId);

    boolean update(FeedbackHistory feedbackHistory);

    boolean softDelete(UUID id);
}
