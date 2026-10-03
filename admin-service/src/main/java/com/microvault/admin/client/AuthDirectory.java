package com.microvault.admin.client;

import com.microvault.admin.dto.UserCard;
import com.microvault.admin.dto.UserWriteRequest;
import com.microvault.admin.exception.ForbiddenException;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.exception.UnauthorizedException;
import com.microvault.admin.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.UUID;

@Component
public class AuthDirectory {

    private final RestClient restClient;
    private final String internalToken;

    public AuthDirectory(@Value("${auth-service.url}") String authServiceUrl,
                         @Value("${microvault.internal.token}") String internalToken) {
        this.restClient = RestClient.builder().baseUrl(authServiceUrl).build();
        this.internalToken = internalToken;
    }

    public SessionUser requireSession(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new UnauthorizedException("Login is required");
        }
        try {
            SessionUser session = restClient.get()
                    .uri("/api/auth/session")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(SessionUser.class);
            if (session == null || session.getUserId() == null) {
                throw new UnauthorizedException("Login is required");
            }
            return session;
        } catch (RestClientResponseException exception) {
            throw new UnauthorizedException("Login is required");
        }
    }

    public SessionUser requireAdmin(String authorization) {
        SessionUser session = requireSession(authorization);
        if (!"admin".equalsIgnoreCase(session.getRole()) && !"superadmin".equalsIgnoreCase(session.getRole())) {
            throw new ForbiddenException("Admin access is required");
        }
        return session;
    }

    public List<UserCard> listUsers(String role) {
        try {
            List<UserCard> users = restClient.get()
                    .uri(builder -> {
                        builder.path("/api/users");
                        if (role != null && !role.isBlank()) {
                            builder.queryParam("role", role);
                        }
                        return builder.build();
                    })
                    .header("X-Internal-Token", internalToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UserCard>>() {
                    });
            return users == null ? List.of() : users;
        } catch (RestClientResponseException exception) {
            throw new ValidationException("Could not load users");
        }
    }

    public UserCard createUser(UserWriteRequest request) {
        try {
            Envelope envelope = restClient.post()
                    .uri("/api/users")
                    .header("X-Internal-Token", internalToken)
                    .body(request)
                    .retrieve()
                    .body(Envelope.class);
            if (envelope == null || envelope.data == null) {
                throw new ValidationException("Could not create the user");
            }
            return envelope.data;
        } catch (RestClientResponseException exception) {
            throw mapped(exception, "Could not create the user");
        }
    }

    public UserCard updateUser(UUID id, UserWriteRequest request) {
        try {
            Envelope envelope = restClient.put()
                    .uri("/api/users/" + id)
                    .header("X-Internal-Token", internalToken)
                    .body(request)
                    .retrieve()
                    .body(Envelope.class);
            if (envelope == null || envelope.data == null) {
                throw new ResourceNotFoundException("User not found");
            }
            return envelope.data;
        } catch (RestClientResponseException exception) {
            throw mapped(exception, "Could not update the user");
        }
    }

    public void deleteUser(UUID id) {
        try {
            restClient.delete()
                    .uri("/api/users/" + id)
                    .header("X-Internal-Token", internalToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw mapped(exception, "Could not delete the user");
        }
    }

    private RuntimeException mapped(RestClientResponseException exception, String fallback) {
        if (exception.getStatusCode().value() == 404) {
            return new ResourceNotFoundException("User not found");
        }
        if (exception.getStatusCode().value() == 409) {
            return new ValidationException("Email is already registered");
        }
        return new ValidationException(fallback);
    }

    public static class SessionUser {
        private UUID userId;
        private String fullName;
        private String email;
        private String role;
        private String status;

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class Envelope {
        public UserCard data;
    }
}
