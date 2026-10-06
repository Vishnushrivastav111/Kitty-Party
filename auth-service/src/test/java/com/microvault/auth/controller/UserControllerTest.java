package com.microvault.auth.controller;

import com.microvault.auth.dto.ChangePasswordRequest;
import com.microvault.auth.dto.CreateUserRequest;
import com.microvault.auth.dto.SessionResponse;
import com.microvault.auth.dto.UpdateUserRequest;
import com.microvault.auth.dto.UserResponse;
import com.microvault.auth.exception.DuplicateResourceException;
import com.microvault.auth.exception.ResourceNotFoundException;
import com.microvault.auth.exception.UnauthorizedException;
import com.microvault.auth.exception.ValidationException;
import com.microvault.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class, properties = "microvault.internal.token=internal-secret")
class UserControllerTest {

    private static final String BEARER = "Bearer jwt";
    private static final String INTERNAL = "internal-secret";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID otherId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    // ================================================================ GET /lookup

    @Test
    void lookupAsAMemberWithoutParametersReturnsTheirOwnAccount() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("user"));
        when(authService.currentUser(BEARER)).thenReturn(user("asha@example.com"));

        mockMvc.perform(get("/api/users/lookup").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha@example.com"));
        verify(authService, never()).lookup(any(), any());
    }

    @Test
    void lookupAsAMemberWithTheirOwnIdAndEmailIsAllowed() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("user"));
        when(authService.currentUser(BEARER)).thenReturn(user("asha@example.com"));

        mockMvc.perform(get("/api/users/lookup")
                        .header("Authorization", BEARER)
                        .param("userId", userId.toString())
                        .param("email", "ASHA@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    void lookupAsAMemberWithABlankEmailIsAllowed() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("user"));
        when(authService.currentUser(BEARER)).thenReturn(user("asha@example.com"));

        mockMvc.perform(get("/api/users/lookup").header("Authorization", BEARER).param("email", "  "))
                .andExpect(status().isOk());
    }

    @Test
    void lookupAsAMemberForAnotherUserIdIsForbidden() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("user"));

        mockMvc.perform(get("/api/users/lookup").header("Authorization", BEARER).param("userId", otherId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You can only access your own account"));
        verify(authService, never()).currentUser(any());
    }

    @Test
    void lookupAsAMemberForAnotherEmailIsForbidden() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("user"));

        mockMvc.perform(get("/api/users/lookup").header("Authorization", BEARER).param("email", "other@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void lookupAsAnAdminWithoutParametersReturnsTheirOwnAccount() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("admin"));
        when(authService.currentUser(BEARER)).thenReturn(user("asha@example.com"));

        mockMvc.perform(get("/api/users/lookup").header("Authorization", BEARER))
                .andExpect(status().isOk());
        verify(authService, never()).lookup(any(), any());
    }

    @Test
    void lookupAsAnAdminWithABlankEmailReturnsTheirOwnAccount() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("superadmin"));
        when(authService.currentUser(BEARER)).thenReturn(user("asha@example.com"));

        mockMvc.perform(get("/api/users/lookup").header("Authorization", BEARER).param("email", " "))
                .andExpect(status().isOk());
    }

    @Test
    void lookupAsAnAdminCanFindAnotherUserById() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("admin"));
        when(authService.lookup(otherId, null)).thenReturn(user("other@example.com"));

        mockMvc.perform(get("/api/users/lookup").header("Authorization", BEARER).param("userId", otherId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("other@example.com"));
    }

    @Test
    void lookupAsAnAdminCanFindAnotherUserByEmail() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("admin"));
        when(authService.lookup(null, "other@example.com")).thenReturn(user("other@example.com"));

        mockMvc.perform(get("/api/users/lookup").header("Authorization", BEARER).param("email", "other@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    void lookupOfAnUnknownUserByAnAdminIsNotFound() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("admin"));
        when(authService.lookup(otherId, null)).thenThrow(new ResourceNotFoundException("No user found for this dashboard"));

        mockMvc.perform(get("/api/users/lookup").header("Authorization", BEARER).param("userId", otherId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void lookupWithoutATokenIsUnauthorized() throws Exception {
        when(authService.session(isNull())).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(get("/api/users/lookup")).andExpect(status().isUnauthorized());
    }

    // ================================================================ GET / (internal list)

    @Test
    void listWithTheInternalTokenReturnsUsers() throws Exception {
        when(authService.listUsers(null)).thenReturn(List.of(user("a@example.com"), user("b@example.com")));

        mockMvc.perform(get("/api/users").header("X-Internal-Token", INTERNAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].email").value("b@example.com"));
    }

    @Test
    void listCanFilterByRole() throws Exception {
        when(authService.listUsers("admin")).thenReturn(List.of(user("a@example.com")));

        mockMvc.perform(get("/api/users").param("role", "admin").header("X-Internal-Token", INTERNAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listReturnsAnEmptyArrayWhenThereAreNoUsers() throws Exception {
        when(authService.listUsers(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/users").header("X-Internal-Token", INTERNAL))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listWithAWrongInternalTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users").header("X-Internal-Token", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Login is required"));
        verifyNoInteractions(authService);
    }

    @Test
    void listWithoutAnInternalTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        verifyNoInteractions(authService);
    }

    // ================================================================ /me

    @Test
    void meReturnsTheCurrentUser() throws Exception {
        when(authService.currentUser(BEARER)).thenReturn(user("asha@example.com"));

        mockMvc.perform(get("/api/users/me").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Asha Verma"));
    }

    @Test
    void meWithoutATokenIsUnauthorized() throws Exception {
        when(authService.currentUser(isNull())).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void updateMeReturnsTheUpdatedProfile() throws Exception {
        when(authService.updateCurrentUser(any(), any(UpdateUserRequest.class))).thenReturn(user("asha@example.com"));

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Asha Kumari\",\"phone\":\"9123456780\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateUserRequest> captor = ArgumentCaptor.forClass(UpdateUserRequest.class);
        verify(authService).updateCurrentUser(any(), captor.capture());
        assertEquals("Asha Kumari", captor.getValue().getFullName());
        assertEquals("9123456780", captor.getValue().getPhone());
    }

    @Test
    void updateMeMapsValidationFailureTo400() throws Exception {
        when(authService.updateCurrentUser(any(), any(UpdateUserRequest.class)))
                .thenThrow(new ValidationException("Enter a valid 10-digit mobile number"));

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter a valid 10-digit mobile number"));
    }

    @Test
    void updateMeMapsDuplicateEmailTo409() throws Exception {
        when(authService.updateCurrentUser(any(), any(UpdateUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Email is already registered"));

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"taken@example.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateMeWithoutATokenIsUnauthorized() throws Exception {
        when(authService.updateCurrentUser(isNull(), any(UpdateUserRequest.class)))
                .thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================ /me/password

    @Test
    void changePasswordReturns204() throws Exception {
        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Old@Pass123\",\"newPassword\":\"New@Pass123\",\"confirmPassword\":\"New@Pass123\"}"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<ChangePasswordRequest> captor = ArgumentCaptor.forClass(ChangePasswordRequest.class);
        verify(authService).changePassword(any(), captor.capture());
        assertEquals("Old@Pass123", captor.getValue().getCurrentPassword());
        assertEquals("New@Pass123", captor.getValue().getNewPassword());
    }

    @Test
    void changePasswordRejectsMissingFields() throws Exception {
        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Current password is required")))
                .andExpect(jsonPath("$.message").value(containsString("New password is required")));
        verifyNoInteractions(authService);
    }

    @Test
    void changePasswordMapsAWrongCurrentPasswordTo400() throws Exception {
        doThrow(new ValidationException("Current password is not correct"))
                .when(authService).changePassword(any(), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"x\",\"newPassword\":\"New@Pass123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is not correct"));
    }

    @Test
    void changePasswordWithoutATokenIsUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Login is required"))
                .when(authService).changePassword(isNull(), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"x\",\"newPassword\":\"New@Pass123\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================ GET /{id}

    @Test
    void getReturnsTheCallersOwnAccount() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("user"));
        when(authService.getUser(userId)).thenReturn(user("asha@example.com"));

        mockMvc.perform(get("/api/users/" + userId).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void getAnotherAccountAsAMemberIsForbidden() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("user"));

        mockMvc.perform(get("/api/users/" + otherId).header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only access your own account"));
        verify(authService, never()).getUser(any());
    }

    @Test
    void getAnotherAccountAsAnAdminIsAllowed() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("admin"));
        when(authService.getUser(otherId)).thenReturn(user("other@example.com"));

        mockMvc.perform(get("/api/users/" + otherId).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("other@example.com"));
    }

    @Test
    void getAnUnknownAccountAsAnAdminIsNotFound() throws Exception {
        when(authService.session(BEARER)).thenReturn(session("superadmin"));
        when(authService.getUser(otherId)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/" + otherId).header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWithoutATokenIsUnauthorized() throws Exception {
        when(authService.session(isNull())).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(get("/api/users/" + userId)).andExpect(status().isUnauthorized());
    }

    // ================================================================ POST / (internal create)

    @Test
    void createReturns201WithTheNewUser() throws Exception {
        when(authService.createUser(any(CreateUserRequest.class))).thenReturn(user("new@example.com"));

        mockMvc.perform(post("/api/users")
                        .header("X-Internal-Token", INTERNAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Admin\",\"email\":\"new@example.com\",\"phone\":\"9876543210\",\"password\":\"Str0ng@Pass\",\"role\":\"admin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.email").value("new@example.com"));

        ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
        verify(authService).createUser(captor.capture());
        assertEquals("admin", captor.getValue().getRole());
        assertEquals("New Admin", captor.getValue().getFullName());
    }

    @Test
    void createRejectsAMissingOrWrongInternalToken() throws Exception {
        String body = "{\"fullName\":\"New Admin\",\"email\":\"new@example.com\",\"phone\":\"9876543210\",\"password\":\"Str0ng@Pass\"}";

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users").header("X-Internal-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(authService);
    }

    @Test
    void createRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("X-Internal-Token", INTERNAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Full name is required")))
                .andExpect(jsonPath("$.message").value(containsString("Email is required")));
        verifyNoInteractions(authService);
    }

    @Test
    void createMapsDuplicateEmailTo409() throws Exception {
        when(authService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Email is already registered"));

        mockMvc.perform(post("/api/users")
                        .header("X-Internal-Token", INTERNAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Admin\",\"email\":\"new@example.com\",\"phone\":\"9876543210\",\"password\":\"Str0ng@Pass\"}"))
                .andExpect(status().isConflict());
    }

    // ================================================================ PUT /{id} and DELETE /{id}

    @Test
    void updateReturnsTheUpdatedUser() throws Exception {
        when(authService.updateUser(any(UUID.class), any(UpdateUserRequest.class))).thenReturn(user("asha@example.com"));

        mockMvc.perform(put("/api/users/" + userId)
                        .header("X-Internal-Token", INTERNAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"inactive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Asha Verma"));

        ArgumentCaptor<UpdateUserRequest> captor = ArgumentCaptor.forClass(UpdateUserRequest.class);
        verify(authService).updateUser(any(UUID.class), captor.capture());
        assertEquals("inactive", captor.getValue().getStatus());
    }

    @Test
    void updateWithoutTheInternalTokenIsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/users/" + userId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(authService);
    }

    @Test
    void updateOfAnUnknownUserIsNotFound() throws Exception {
        when(authService.updateUser(any(UUID.class), any(UpdateUserRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/" + userId)
                        .header("X-Internal-Token", INTERNAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/users/" + userId).header("X-Internal-Token", INTERNAL))
                .andExpect(status().isNoContent());

        verify(authService).softDelete(userId);
    }

    @Test
    void deleteWithoutTheInternalTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/" + userId)).andExpect(status().isUnauthorized());
        verifyNoInteractions(authService);
    }

    @Test
    void deleteOfTheSuperAdminIsRejectedWith400() throws Exception {
        doThrow(new ValidationException("The super admin account cannot be deleted"))
                .when(authService).softDelete(userId);

        mockMvc.perform(delete("/api/users/" + userId).header("X-Internal-Token", INTERNAL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The super admin account cannot be deleted"));
    }

    @Test
    void deleteOfAnUnknownUserIsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found")).when(authService).softDelete(userId);

        mockMvc.perform(delete("/api/users/" + userId).header("X-Internal-Token", INTERNAL))
                .andExpect(status().isNotFound());
    }

    // ================================================================ helpers

    private SessionResponse session(String role) {
        SessionResponse session = new SessionResponse();
        session.setUserId(userId);
        session.setEmail("asha@example.com");
        session.setRole(role);
        session.setStatus("active");
        return session;
    }

    private UserResponse user(String email) {
        UserResponse user = new UserResponse();
        user.setId(email.startsWith("asha") ? userId : otherId);
        user.setFullName("Asha Verma");
        user.setEmail(email);
        user.setRole("user");
        user.setStatus("active");
        return user;
    }
}
