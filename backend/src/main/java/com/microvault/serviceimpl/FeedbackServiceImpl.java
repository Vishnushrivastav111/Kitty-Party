package com.microvault.serviceimpl;

import com.microvault.dao.FeedbackDAO;
import com.microvault.dao.FeedbackHistoryDAO;
import com.microvault.dto.FeedbackDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.Feedback;
import com.microvault.model.FeedbackHistory;
import com.microvault.service.FeedbackService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for feedback. Two DAOs are received through the constructor
 * because every change to a feedback item is also written to the audit trail.
 */
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackDAO feedbackDAO;
    private final FeedbackHistoryDAO feedbackHistoryDAO;

    public FeedbackServiceImpl(FeedbackDAO feedbackDAO, FeedbackHistoryDAO feedbackHistoryDAO) {
        this.feedbackDAO = feedbackDAO;
        this.feedbackHistoryDAO = feedbackHistoryDAO;
    }

    @Override
    public FeedbackDTO createFeedback(Feedback feedback) {

        validateFeedback(feedback);

        if (feedback.getStatus() == null || feedback.getStatus().trim().isEmpty()) {
            feedback.setStatus("open");
        }

        Feedback savedFeedback = feedbackDAO.create(feedback);

        // Every new feedback item starts its audit trail with a "created" line.
        FeedbackHistory feedbackHistory = new FeedbackHistory(
                savedFeedback.getId(),
                "created",
                "Feedback created with status " + savedFeedback.getStatus(),
                savedFeedback.getUserId(),
                LocalDateTime.now());
        feedbackHistoryDAO.create(feedbackHistory);

        return toDTO(savedFeedback);
    }

    @Override
    public FeedbackDTO getFeedbackById(UUID id) {
        if (id == null) {
            throw new ValidationException("Feedback id is required");
        }
        Feedback feedback = feedbackDAO.findById(id);
        if (feedback == null) {
            throw new ValidationException("No active feedback found with id " + id);
        }
        return toDTO(feedback);
    }

    @Override
    public List<FeedbackDTO> getAllFeedback() {
        return toDTOList(feedbackDAO.findAll());
    }

    @Override
    public List<FeedbackDTO> getFeedbackByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toDTOList(feedbackDAO.findByUserId(userId));
    }

    @Override
    public List<FeedbackDTO> getFeedbackByStatus(String status) {
        validateStatus(status);
        return toDTOList(feedbackDAO.findByStatus(status));
    }

    @Override
    public List<FeedbackDTO> getAllFeedbackIncludingDeleted() {
        return toDTOList(feedbackDAO.findAllIncludingDeleted());
    }

    @Override
    public boolean updateFeedback(Feedback feedback) {

        if (feedback == null || feedback.getId() == null) {
            throw new ValidationException("Feedback id is required for an update");
        }
        validateFeedback(feedback);

        Feedback existingFeedback = feedbackDAO.findById(feedback.getId());
        if (existingFeedback == null) {
            throw new ValidationException("No active feedback found with id " + feedback.getId());
        }

        if (feedback.getStatus() == null || feedback.getStatus().trim().isEmpty()) {
            feedback.setStatus(existingFeedback.getStatus());
        }

        return feedbackDAO.update(feedback);
    }

    @Override
    public boolean changeStatus(UUID feedbackId, String newStatus, UUID changedBy) {

        if (feedbackId == null) {
            throw new ValidationException("Feedback id is required");
        }
        validateStatus(newStatus);

        Feedback feedback = feedbackDAO.findById(feedbackId);
        if (feedback == null) {
            throw new ValidationException("No active feedback found with id " + feedbackId);
        }

        String oldStatus = feedback.getStatus();

        boolean statusUpdated = feedbackDAO.updateStatus(feedbackId, newStatus);
        if (!statusUpdated) {
            return false;
        }

        // The audit trail keeps both the old and the new status.
        FeedbackHistory feedbackHistory = new FeedbackHistory(
                feedbackId,
                "status changed",
                "Status changed from " + oldStatus + " to " + newStatus,
                changedBy,
                LocalDateTime.now());
        feedbackHistoryDAO.create(feedbackHistory);

        return true;
    }

    @Override
    public boolean softDeleteFeedback(UUID id) {
        if (id == null) {
            throw new ValidationException("Feedback id is required");
        }
        if (feedbackDAO.findById(id) == null) {
            throw new ValidationException("No active feedback found with id " + id);
        }
        return feedbackDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateFeedback(Feedback feedback) {

        if (feedback == null) {
            throw new ValidationException("Feedback is required");
        }
        if (feedback.getUserId() == null) {
            throw new ValidationException("User id is required");
        }
        if (feedback.getSubject() == null || feedback.getSubject().trim().isEmpty()) {
            throw new ValidationException("Subject is required");
        }
        if (feedback.getCategory() == null || feedback.getCategory().trim().isEmpty()) {
            throw new ValidationException("Category is required");
        }
        if (feedback.getMessage() == null || feedback.getMessage().trim().isEmpty()) {
            throw new ValidationException("Message is required");
        }
        if (feedback.getMessage().trim().length() < 10) {
            throw new ValidationException("Message must be at least 10 characters");
        }
        if (feedback.getStatus() != null && !feedback.getStatus().trim().isEmpty()
                && !feedback.getStatus().matches("open|in-review|resolved|closed")) {
            throw new ValidationException("Status must be open, in-review, resolved or closed");
        }
    }

    private void validateStatus(String status) {

        if (status == null || status.trim().isEmpty()
                || !status.matches("open|in-review|resolved|closed")) {
            throw new ValidationException("Status must be open, in-review, resolved or closed");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private FeedbackDTO toDTO(Feedback feedback) {

        FeedbackDTO feedbackDTO = new FeedbackDTO();
        feedbackDTO.setId(feedback.getId());
        feedbackDTO.setUserId(feedback.getUserId());
        feedbackDTO.setSubject(feedback.getSubject());
        feedbackDTO.setCategory(feedback.getCategory());
        feedbackDTO.setMessage(feedback.getMessage());
        feedbackDTO.setStatus(feedback.getStatus());
        feedbackDTO.setCreatedAt(feedback.getCreatedAt());
        feedbackDTO.setDeletedAt(feedback.getDeletedAt());
        return feedbackDTO;
    }

    private List<FeedbackDTO> toDTOList(List<Feedback> feedbackList) {
        List<FeedbackDTO> feedbackDTOs = new ArrayList<>();
        for (Feedback feedback : feedbackList) {
            feedbackDTOs.add(toDTO(feedback));
        }
        return feedbackDTOs;
    }
}
