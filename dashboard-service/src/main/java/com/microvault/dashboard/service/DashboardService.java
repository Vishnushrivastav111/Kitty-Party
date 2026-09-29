package com.microvault.dashboard.service;

import com.microvault.dashboard.model.DashboardData;

import java.util.UUID;

/** Builds the single payload the dashboard asks for when the page opens. */
public interface DashboardService {

    DashboardData load(UUID userId, String email);
}
