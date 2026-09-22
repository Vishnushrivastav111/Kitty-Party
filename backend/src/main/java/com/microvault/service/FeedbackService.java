package com.microvault.service;

import com.microvault.dto.FeedbackDTO;
import com.microvault.model.Feedback;

import java.util.List;
import java.util.UUID;

/**
 * Business operations for feedback: validation, status changes and the audit
 * trail that goes with every change.
 */
public interface FeedbackService {

    FeedbackDTO createFeedback(Feedback feedback);

    FeedbackDTO getFeedbackById(UUID id);

    List<FeedbackDTO> getAllFeedback();

    List<FeedbackDTO> getFeedbackByUserId(UUID userId);

    List<FeedbackDTO> getFeedbackByStatus(String status);

    List<FeedbackDTO> getAllFeedbackIncludingDeleted();

    boolean updateFeedback(Feedback feedback);

    boolean changeStatus(UUID feedbackId, String newStatus, UUID changedBy);

    boolean softDeleteFeedback(UUID id);
}
