package com.microvault.serviceimpl;

import com.microvault.dao.FeedbackHistoryDAO;
import com.microvault.dto.FeedbackHistoryDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.FeedbackHistory;
import com.microvault.service.FeedbackHistoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for the feedback audit trail. The DAO is received through the
 * constructor, so this class is bound to the FeedbackHistoryDAO interface and
 * not to a concrete class.
 */
public class FeedbackHistoryServiceImpl implements FeedbackHistoryService {

    private final FeedbackHistoryDAO feedbackHistoryDAO;

    public FeedbackHistoryServiceImpl(FeedbackHistoryDAO feedbackHistoryDAO) {
        this.feedbackHistoryDAO = feedbackHistoryDAO;
    }

    @Override
    public FeedbackHistoryDTO createHistory(FeedbackHistory feedbackHistory) {

        validateFeedbackHistory(feedbackHistory);

        FeedbackHistory savedFeedbackHistory = feedbackHistoryDAO.create(feedbackHistory);
        return toDTO(savedFeedbackHistory);
    }

    @Override
    public FeedbackHistoryDTO getHistoryById(UUID id) {
        if (id == null) {
            throw new ValidationException("Feedback history id is required");
        }
        FeedbackHistory feedbackHistory = feedbackHistoryDAO.findById(id);
        if (feedbackHistory == null) {
            throw new ValidationException("No active feedback history found with id " + id);
        }
        return toDTO(feedbackHistory);
    }

    @Override
    public List<FeedbackHistoryDTO> getAllHistory() {
        return toDTOList(feedbackHistoryDAO.findAll());
    }

    @Override
    public List<FeedbackHistoryDTO> getHistoryByFeedbackId(UUID feedbackId) {
        if (feedbackId == null) {
            throw new ValidationException("Feedback id is required");
        }
        return toDTOList(feedbackHistoryDAO.findByFeedbackId(feedbackId));
    }

    @Override
    public boolean updateHistory(FeedbackHistory feedbackHistory) {

        if (feedbackHistory == null || feedbackHistory.getId() == null) {
            throw new ValidationException("Feedback history id is required for an update");
        }
        validateFeedbackHistory(feedbackHistory);

        FeedbackHistory existingFeedbackHistory = feedbackHistoryDAO.findById(feedbackHistory.getId());
        if (existingFeedbackHistory == null) {
            throw new ValidationException("No active feedback history found with id " + feedbackHistory.getId());
        }

        return feedbackHistoryDAO.update(feedbackHistory);
    }

    @Override
    public boolean softDeleteHistory(UUID id) {
        if (id == null) {
            throw new ValidationException("Feedback history id is required");
        }
        if (feedbackHistoryDAO.findById(id) == null) {
            throw new ValidationException("No active feedback history found with id " + id);
        }
        return feedbackHistoryDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateFeedbackHistory(FeedbackHistory feedbackHistory) {

        if (feedbackHistory == null) {
            throw new ValidationException("Feedback history is required");
        }
        if (feedbackHistory.getFeedbackId() == null) {
            throw new ValidationException("Feedback id is required");
        }
        if (feedbackHistory.getAction() == null || feedbackHistory.getAction().trim().isEmpty()) {
            throw new ValidationException("Action is required");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private FeedbackHistoryDTO toDTO(FeedbackHistory feedbackHistory) {

        FeedbackHistoryDTO feedbackHistoryDTO = new FeedbackHistoryDTO();
        feedbackHistoryDTO.setId(feedbackHistory.getId());
        feedbackHistoryDTO.setFeedbackId(feedbackHistory.getFeedbackId());
        feedbackHistoryDTO.setAction(feedbackHistory.getAction());
        feedbackHistoryDTO.setNote(feedbackHistory.getNote());
        feedbackHistoryDTO.setChangedBy(feedbackHistory.getChangedBy());
        feedbackHistoryDTO.setChangedAt(feedbackHistory.getChangedAt());
        return feedbackHistoryDTO;
    }

    private List<FeedbackHistoryDTO> toDTOList(List<FeedbackHistory> feedbackHistoryList) {
        List<FeedbackHistoryDTO> feedbackHistoryDTOs = new ArrayList<>();
        for (FeedbackHistory feedbackHistory : feedbackHistoryList) {
            feedbackHistoryDTOs.add(toDTO(feedbackHistory));
        }
        return feedbackHistoryDTOs;
    }
}
