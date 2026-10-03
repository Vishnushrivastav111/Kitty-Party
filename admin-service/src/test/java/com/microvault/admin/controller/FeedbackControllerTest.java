package com.microvault.admin.controller;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.dto.FeedbackResponse;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AuthDirectory authDirectory;

    @Test
    void createReturnsFeedback() throws Exception {
        AuthDirectory.SessionUser user = new AuthDirectory.SessionUser();
        user.setUserId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);
        FeedbackResponse response = new FeedbackResponse();
        response.setSubject("App idea");
        response.setStatus("open");
        when(adminService.addFeedback(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"App idea\",\"message\":\"Please add export\",\"category\":\"Idea\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("App idea"));
    }

    @Test
    void createRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Hi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingUserIsNotFoundOnHistory() throws Exception {
        when(adminService.history(any())).thenThrow(new ResourceNotFoundException("Feedback not found"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/feedback/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Feedback not found"));
    }
}
