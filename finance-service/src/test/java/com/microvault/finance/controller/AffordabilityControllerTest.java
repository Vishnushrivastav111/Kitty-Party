package com.microvault.finance.controller;

import com.microvault.finance.dto.AffordabilityResponse;
import com.microvault.finance.service.AffordabilityService;
import com.microvault.finance.service.RequestUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AffordabilityController.class)
class AffordabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AffordabilityService affordabilityService;

    @MockBean
    private RequestUser requestUser;

    @Test
    void checkReturnsPlan() throws Exception {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(requestUser.requireUser("Bearer token")).thenReturn(userId);
        AffordabilityResponse response = new AffordabilityResponse();
        response.setVerdict("Comfortably affordable");
        response.setAvailable(new BigDecimal("33000.00"));
        response.setSuggestion("Buy now.");
        response.setPlan(List.of("Available capacity is enough."));
        when(affordabilityService.check(eq(userId), any())).thenReturn(response);

        mockMvc.perform(post("/api/affordability/check")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"Headphones\",\"amount\":3000,\"checkDate\":\"2026-10-01\",\"priority\":\"Want\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verdict").value("Comfortably affordable"))
                .andExpect(jsonPath("$.plan[0]").value("Available capacity is enough."));
    }

    @Test
    void checkRejectsMissingItem() throws Exception {
        mockMvc.perform(post("/api/affordability/check")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10,\"checkDate\":\"2026-10-01\"}"))
                .andExpect(status().isBadRequest());
    }
}
