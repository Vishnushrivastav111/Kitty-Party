package com.microvault.dashboard.service;

import com.microvault.dashboard.dto.DashboardResponse;

public interface DashboardService {
    DashboardResponse load(String authorization);
}
