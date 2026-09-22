package com.microvault.dao;

import com.microvault.model.Feedback;

import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "feedback" table. Only data access is described
 * here, no business rules.
 */
public interface FeedbackDAO {

    Feedback create(Feedback feedback);

    Feedback findById(UUID id);

    List<Feedback> findAll();

    List<Feedback> findByUserId(UUID userId);

    List<Feedback> findByStatus(String status);

    /** Admin audit view: this is the only query that also returns deleted rows. */
    List<Feedback> findAllIncludingDeleted();

    boolean update(Feedback feedback);

    boolean updateStatus(UUID id, String status);

    boolean softDelete(UUID id);
}
