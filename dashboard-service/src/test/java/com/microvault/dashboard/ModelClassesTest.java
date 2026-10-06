package com.microvault.dashboard;

import com.microvault.dashboard.dto.DashboardResponse;
import com.microvault.dashboard.dto.DashboardStats;
import com.microvault.dashboard.exception.ErrorResponse;
import com.microvault.dashboard.exception.ResourceNotFoundException;
import com.microvault.dashboard.exception.UnauthorizedException;
import com.microvault.dashboard.exception.ValidationException;
import com.microvault.dashboard.support.BeanVerifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelClassesTest {

    @Test
    void dashboardResponseRoundTripsEveryProperty() throws Exception {
        assertTrue(BeanVerifier.verify(DashboardResponse.class) >= 15);
    }

    @Test
    void dashboardStatsRoundTripsEveryProperty() throws Exception {
        assertEquals(8, BeanVerifier.verify(DashboardStats.class));
    }

    @Test
    void errorResponseCarriesStatusMessageAndPath() {
        ErrorResponse error = ErrorResponse.of(404, "missing", "/api/x");

        assertEquals(404, error.getStatus());
        assertEquals("missing", error.getMessage());
        assertEquals("/api/x", error.getPath());
        assertNotNull(error.getTimestamp());
    }

    @Test
    void exceptionsKeepTheirMessage() {
        assertEquals("a", new ResourceNotFoundException("a").getMessage());
        assertEquals("b", new UnauthorizedException("b").getMessage());
        assertEquals("c", new ValidationException("c").getMessage());
    }
}
