package com.microvault.admin.controller;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.dto.NotificationResponse;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.exception.UnauthorizedException;
import com.microvault.admin.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    private static final String ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID UUID_ID = UUID.fromString(ID);
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AuthDirectory authDirectory;

    @BeforeEach
    void setUp() {
        AuthDirectory.SessionUser user = new AuthDirectory.SessionUser();
        user.setUserId(USER_ID);
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);
        when(authDirectory.requireSession(null)).thenThrow(new UnauthorizedException("Login is required"));
    }

    @Test
    void listReturnsMyNotifications() throws Exception {
        NotificationResponse notification = new NotificationResponse();
        notification.setTitle("Welcome");
        notification.setRead(true);
        when(adminService.notifications(USER_ID)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Welcome"))
                .andExpect(jsonPath("$[0].read").value(true));
    }

    @Test
    void listCanBeEmpty() throws Exception {
        when(adminService.notifications(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listWithoutALoginIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Login is required"));
        verify(adminService, never()).notifications(any());
    }

    @Test
    void readAllReturns204() throws Exception {
        mockMvc.perform(put("/api/notifications/read-all").header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());
        verify(adminService).markAllRead(USER_ID);
    }

    @Test
    void readAllWithoutALoginIsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/notifications/read-all")).andExpect(status().isUnauthorized());
        verify(adminService, never()).markAllRead(any());
    }

    @Test
    void readOneReturns204() throws Exception {
        mockMvc.perform(put("/api/notifications/" + ID + "/read").header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());
        verify(adminService).markRead(USER_ID, UUID_ID);
    }

    @Test
    void readUnknownNotificationIs404() throws Exception {
        doThrow(new ResourceNotFoundException("Notification not found")).when(adminService).markRead(USER_ID, UUID_ID);

        mockMvc.perform(put("/api/notifications/" + ID + "/read").header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found"));
    }

    @Test
    void readOneWithoutALoginIsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/notifications/" + ID + "/read")).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteOneReturns204() throws Exception {
        mockMvc.perform(delete("/api/notifications/" + ID).header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());
        verify(adminService).deleteNotification(USER_ID, UUID_ID);
    }

    @Test
    void deleteUnknownNotificationIs404() throws Exception {
        doThrow(new ResourceNotFoundException("Notification not found"))
                .when(adminService).deleteNotification(USER_ID, UUID_ID);

        mockMvc.perform(delete("/api/notifications/" + ID).header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOneWithoutALoginIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/notifications/" + ID)).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAllReturns204() throws Exception {
        mockMvc.perform(delete("/api/notifications").header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());
        verify(adminService).deleteAllNotifications(USER_ID);
    }

    @Test
    void deleteAllWithoutALoginIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void anUnexpectedFailureIs500() throws Exception {
        when(adminService.notifications(USER_ID)).thenThrow(new IllegalStateException("db"));

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }
}
