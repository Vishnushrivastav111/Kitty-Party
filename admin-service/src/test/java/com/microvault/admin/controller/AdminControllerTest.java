package com.microvault.admin.controller;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.dto.AdminWorkspaceResponse;
import com.microvault.admin.dto.FeedbackResponse;
import com.microvault.admin.dto.NewsRequest;
import com.microvault.admin.dto.NewsResponse;
import com.microvault.admin.dto.UserCard;
import com.microvault.admin.dto.UserWriteRequest;
import com.microvault.admin.exception.ForbiddenException;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.exception.UnauthorizedException;
import com.microvault.admin.exception.ValidationException;
import com.microvault.admin.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AdminController.class, properties = "microvault.internal.token=internal-secret")
class AdminControllerTest {

    private static final String ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID UUID_ID = UUID.fromString(ID);
    private static final String BEARER = "Bearer admin-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AuthDirectory authDirectory;

    private AuthDirectory.SessionUser admin;

    @BeforeEach
    void setUp() {
        admin = new AuthDirectory.SessionUser();
        admin.setUserId(UUID_ID);
        admin.setFullName("Root Admin");
        admin.setRole("admin");
    }

    private UserCard card() {
        UserCard card = new UserCard();
        card.setId(UUID_ID);
        card.setFullName("Asha Rao");
        card.setEmail("asha@example.com");
        return card;
    }

    // ------------------------------------------------------------------ workspace (internal token)

    @Test
    void workspaceReturnsTheSliceForAnInternalCaller() throws Exception {
        AdminWorkspaceResponse workspace = new AdminWorkspaceResponse();
        FeedbackResponse feedback = new FeedbackResponse();
        feedback.setSubject("Idea");
        workspace.setFeedback(List.of(feedback));
        when(adminService.workspace(UUID_ID, "admin")).thenReturn(workspace);

        mockMvc.perform(get("/api/admin/workspace")
                        .param("userId", ID).param("role", "admin")
                        .header("X-Internal-Token", "internal-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback[0].subject").value("Idea"));
    }

    @Test
    void workspaceRoleIsOptional() throws Exception {
        when(adminService.workspace(UUID_ID, null)).thenReturn(new AdminWorkspaceResponse());

        mockMvc.perform(get("/api/admin/workspace").param("userId", ID).header("X-Internal-Token", "internal-secret"))
                .andExpect(status().isOk());
    }

    @Test
    void workspaceRejectsAWrongInternalToken() throws Exception {
        mockMvc.perform(get("/api/admin/workspace").param("userId", ID).header("X-Internal-Token", "nope"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Login is required"));
        verify(adminService, never()).workspace(any(), any());
    }

    @Test
    void workspaceRejectsAMissingInternalToken() throws Exception {
        mockMvc.perform(get("/api/admin/workspace").param("userId", ID))
                .andExpect(status().isUnauthorized());
        verify(adminService, never()).workspace(any(), any());
    }

    // ------------------------------------------------------------------ members

    @Test
    void usersListsMembersForAnAdmin() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.members()).thenReturn(List.of(card()));

        mockMvc.perform(get("/api/admin/users").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Asha Rao"));
    }

    @Test
    void usersWithoutALoginIsUnauthorized() throws Exception {
        when(authDirectory.requireAdmin(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
        verify(adminService, never()).members();
    }

    @Test
    void usersForAMemberIsForbidden() throws Exception {
        when(authDirectory.requireAdmin("Bearer member")).thenThrow(new ForbiddenException("Admin access is required"));

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer member"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Admin access is required"));
    }

    @Test
    void usersServiceFailureIsA500() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.members()).thenThrow(new IllegalStateException("db"));

        mockMvc.perform(get("/api/admin/users").header("Authorization", BEARER))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    @Test
    void updateUserReturnsTheUpdatedMember() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.updateUser(eq(UUID_ID), any(UserWriteRequest.class))).thenReturn(card());

        mockMvc.perform(put("/api/admin/users/" + ID).header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Asha Rao\",\"status\":\"inactive\",\"ignored\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha@example.com"));
        ArgumentCaptor<UserWriteRequest> captor = ArgumentCaptor.forClass(UserWriteRequest.class);
        verify(adminService).updateUser(eq(UUID_ID), captor.capture());
        assertEquals("inactive", captor.getValue().getStatus());
    }

    @Test
    void updateUserNotFoundIs404() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.updateUser(eq(UUID_ID), any())).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/admin/users/" + ID).header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void updateUserWithMalformedJsonIs400() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);

        mockMvc.perform(put("/api/admin/users/" + ID).header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is not valid"));
    }

    @Test
    void updateUserRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(put("/api/admin/users/" + ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verify(adminService, never()).updateUser(any(), any());
    }

    @Test
    void deleteUserReturns204() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);

        mockMvc.perform(delete("/api/admin/users/" + ID).header("Authorization", BEARER))
                .andExpect(status().isNoContent());
        verify(adminService).deleteUser(UUID_ID);
    }

    @Test
    void deleteUserRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin("Bearer member")).thenThrow(new ForbiddenException("Admin access is required"));

        mockMvc.perform(delete("/api/admin/users/" + ID).header("Authorization", "Bearer member"))
                .andExpect(status().isForbidden());
        verify(adminService, never()).deleteUser(any());
    }

    // ------------------------------------------------------------------ admins

    @Test
    void adminsListsAdmins() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.admins()).thenReturn(List.of(card()));

        mockMvc.perform(get("/api/admin/admins").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ID));
    }

    @Test
    void adminsRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(get("/api/admin/admins")).andExpect(status().isUnauthorized());
    }

    @Test
    void createAdminReturns201() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.createAdmin(any(UserWriteRequest.class))).thenReturn(card());

        mockMvc.perform(post("/api/admin/admins").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Admin\",\"email\":\"new@example.com\",\"password\":\"Str0ng@Pass\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Asha Rao"));
    }

    @Test
    void createAdminDuplicateEmailIs400() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.createAdmin(any())).thenThrow(new ValidationException("Email is already registered"));

        mockMvc.perform(post("/api/admin/admins").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void createAdminRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin("Bearer member")).thenThrow(new ForbiddenException("Admin access is required"));

        mockMvc.perform(post("/api/admin/admins").header("Authorization", "Bearer member")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verify(adminService, never()).createAdmin(any());
    }

    @Test
    void updateAdminDefaultsABlankRoleToAdmin() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.updateUser(eq(UUID_ID), any())).thenReturn(card());

        mockMvc.perform(put("/api/admin/admins/" + ID).header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"  \"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<UserWriteRequest> captor = ArgumentCaptor.forClass(UserWriteRequest.class);
        verify(adminService).updateUser(eq(UUID_ID), captor.capture());
        assertEquals("admin", captor.getValue().getRole());
    }

    @Test
    void updateAdminDefaultsAMissingRoleToAdmin() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.updateUser(eq(UUID_ID), any())).thenReturn(card());

        mockMvc.perform(put("/api/admin/admins/" + ID).header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        ArgumentCaptor<UserWriteRequest> captor = ArgumentCaptor.forClass(UserWriteRequest.class);
        verify(adminService).updateUser(eq(UUID_ID), captor.capture());
        assertEquals("admin", captor.getValue().getRole());
    }

    @Test
    void updateAdminKeepsAnExplicitRole() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.updateUser(eq(UUID_ID), any())).thenReturn(card());

        mockMvc.perform(put("/api/admin/admins/" + ID).header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"user\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<UserWriteRequest> captor = ArgumentCaptor.forClass(UserWriteRequest.class);
        verify(adminService).updateUser(eq(UUID_ID), captor.capture());
        assertEquals("user", captor.getValue().getRole());
    }

    @Test
    void updateAdminRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(put("/api/admin/admins/" + ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAdminReturns204() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);

        mockMvc.perform(delete("/api/admin/admins/" + ID).header("Authorization", BEARER))
                .andExpect(status().isNoContent());
        verify(adminService).deleteUser(UUID_ID);
    }

    @Test
    void deleteAdminNotFoundIs404() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("User not found")).when(adminService).deleteUser(UUID_ID);

        mockMvc.perform(delete("/api/admin/admins/" + ID).header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAdminRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(delete("/api/admin/admins/" + ID)).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ feedback and news

    @Test
    void feedbackListsEveryItemForAnAdmin() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        FeedbackResponse feedback = new FeedbackResponse();
        feedback.setSubject("Idea");
        when(adminService.allFeedback()).thenReturn(List.of(feedback));

        mockMvc.perform(get("/api/admin/feedback").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Idea"));
    }

    @Test
    void feedbackRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin("Bearer member")).thenThrow(new ForbiddenException("Admin access is required"));

        mockMvc.perform(get("/api/admin/feedback").header("Authorization", "Bearer member"))
                .andExpect(status().isForbidden());
    }

    @Test
    void newsListsEveryItemForAnAdmin() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        NewsResponse news = new NewsResponse();
        news.setTitle("Update");
        when(adminService.allNews()).thenReturn(List.of(news));

        mockMvc.perform(get("/api/admin/news").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Update"));
    }

    @Test
    void newsRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(get("/api/admin/news")).andExpect(status().isUnauthorized());
    }

    @Test
    void createNewsReturns201() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        NewsResponse created = new NewsResponse();
        created.setTitle("Update");
        when(adminService.addNews(eq(admin), any(NewsRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/admin/news").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Update\",\"body\":\"Details\",\"priority\":\"high\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Update"));
    }

    @Test
    void createNewsValidatesTitleAndBody() throws Exception {
        mockMvc.perform(post("/api/admin/news").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"priority\":\"high\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(adminService, never()).addNews(any(), any());
    }

    @Test
    void createNewsRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin("Bearer member")).thenThrow(new ForbiddenException("Admin access is required"));

        mockMvc.perform(post("/api/admin/news").header("Authorization", "Bearer member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Update\",\"body\":\"Details\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createNewsWithABadPriorityIs400() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.addNews(eq(admin), any())).thenThrow(new ValidationException("Priority must be low, normal or high"));

        mockMvc.perform(post("/api/admin/news").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Update\",\"body\":\"Details\",\"priority\":\"urgent\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Priority must be low, normal or high"));
    }

    @Test
    void updateNewsReturnsTheUpdatedItem() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        NewsResponse updated = new NewsResponse();
        updated.setTitle("Changed");
        when(adminService.updateNews(eq(UUID_ID), any(NewsRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/admin/news/" + ID).header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Changed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Changed"));
    }

    @Test
    void updateNewsNotFoundIs404() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        when(adminService.updateNews(eq(UUID_ID), any())).thenThrow(new ResourceNotFoundException("News item not found"));

        mockMvc.perform(put("/api/admin/news/" + ID).header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("News item not found"));
    }

    @Test
    void updateNewsRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(put("/api/admin/news/" + ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteNewsReturns204() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);

        mockMvc.perform(delete("/api/admin/news/" + ID).header("Authorization", BEARER))
                .andExpect(status().isNoContent());
        verify(adminService).deleteNews(UUID_ID);
    }

    @Test
    void deleteNewsNotFoundIs404() throws Exception {
        when(authDirectory.requireAdmin(BEARER)).thenReturn(admin);
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("News item not found")).when(adminService).deleteNews(UUID_ID);

        mockMvc.perform(delete("/api/admin/news/" + ID).header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNewsRequiresAnAdmin() throws Exception {
        when(authDirectory.requireAdmin("Bearer member")).thenThrow(new ForbiddenException("Admin access is required"));

        mockMvc.perform(delete("/api/admin/news/" + ID).header("Authorization", "Bearer member"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ CORS

    @Test
    void corsPreflightIsAllowedForAnyOrigin() throws Exception {
        mockMvc.perform(options("/api/admin/users")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
