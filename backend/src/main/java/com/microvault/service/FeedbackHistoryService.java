package com.microvault.service;

import com.microvault.dto.FeedbackHistoryDTO;
import com.microvault.model.FeedbackHistory;

import java.util.List;
import java.util.UUID;

/**
 * Business operations for the feedback audit trail.
 */
public interface FeedbackHistoryService {

    FeedbackHistoryDTO createHistory(FeedbackHistory feedbackHistory);

    FeedbackHistoryDTO getHistoryById(UUID id);

    List<FeedbackHistoryDTO> getAllHistory();

    List<FeedbackHistoryDTO> getHistoryByFeedbackId(UUID feedbackId);

    boolean updateHistory(FeedbackHistory feedbackHistory);

    boolean softDeleteHistory(UUID id);
}
