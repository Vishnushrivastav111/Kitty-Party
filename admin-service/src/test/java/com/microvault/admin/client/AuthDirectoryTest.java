package com.microvault.admin.client;

import com.microvault.admin.dto.UserCard;
import com.microvault.admin.dto.UserWriteRequest;
import com.microvault.admin.exception.ForbiddenException;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.exception.UnauthorizedException;
import com.microvault.admin.exception.ValidationException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Talks to a real in-process HTTP server so the RestClient wiring and error mapping are exercised end to end. */
class AuthDirectoryTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private HttpServer server;
    private AuthDirectory directory;
    private final AtomicReference<String> seenMethod = new AtomicReference<>();
    private final AtomicReference<String> seenPath = new AtomicReference<>();
    private final AtomicReference<String> seenQuery = new AtomicReference<>();
    private final AtomicReference<String> seenAuthorization = new AtomicReference<>();
    private final AtomicReference<String> seenToken = new AtomicReference<>();
    private final AtomicReference<String> seenBody = new AtomicReference<>();
    private volatile int status;
    private volatile String body;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            seenMethod.set(exchange.getRequestMethod());
            seenPath.set(exchange.getRequestURI().getPath());
            seenQuery.set(exchange.getRequestURI().getQuery());
            seenAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            seenToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            seenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, payload.length == 0 ? -1 : payload.length);
            if (payload.length > 0) {
                exchange.getResponseBody().write(payload);
            }
            exchange.close();
        });
        server.start();
        directory = new AuthDirectory("http://127.0.0.1:" + server.getAddress().getPort(), "internal-secret");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String sessionJson(String role) {
        return "{\"userId\":\"" + USER_ID + "\",\"fullName\":\"Asha Rao\",\"email\":\"asha@example.com\","
                + "\"role\":" + (role == null ? "null" : "\"" + role + "\"") + ",\"status\":\"active\",\"extra\":1}";
    }

    private String userJson() {
        return "{\"id\":\"" + USER_ID + "\",\"fullName\":\"Asha Rao\",\"email\":\"asha@example.com\","
                + "\"role\":\"user\",\"status\":\"active\",\"unknownField\":true}";
    }

    // ------------------------------------------------------------------ requireSession

    @Test
    void requireSessionReturnsTheSessionAndForwardsTheToken() {
        status = 200;
        body = sessionJson("user");

        AuthDirectory.SessionUser session = directory.requireSession("Bearer good");

        assertEquals(USER_ID, session.getUserId());
        assertEquals("Asha Rao", session.getFullName());
        assertEquals("asha@example.com", session.getEmail());
        assertEquals("user", session.getRole());
        assertEquals("active", session.getStatus());
        assertEquals("Bearer good", seenAuthorization.get());
        assertEquals("/api/auth/session", seenPath.get());
        assertEquals("GET", seenMethod.get());
    }

    @Test
    void requireSessionRejectsAMissingHeaderWithoutCallingTheServer() {
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> directory.requireSession(null));

        assertEquals("Login is required", exception.getMessage());
        assertNull(seenPath.get());
    }

    @Test
    void requireSessionRejectsABlankHeaderWithoutCallingTheServer() {
        assertThrows(UnauthorizedException.class, () -> directory.requireSession("   "));
        assertNull(seenPath.get());
    }

    @Test
    void requireSessionRejectsAnEmptyResponse() {
        status = 200;
        body = null;

        assertThrows(UnauthorizedException.class, () -> directory.requireSession("Bearer good"));
    }

    @Test
    void requireSessionRejectsASessionWithoutAUserId() {
        status = 200;
        body = "{\"fullName\":\"Nobody\"}";

        assertThrows(UnauthorizedException.class, () -> directory.requireSession("Bearer good"));
    }

    @Test
    void requireSessionRejectsAnUnauthorizedResponse() {
        status = 401;
        body = "{}";

        assertThrows(UnauthorizedException.class, () -> directory.requireSession("Bearer expired"));
    }

    @Test
    void requireSessionTurnsAnyServerErrorIntoUnauthorized() {
        status = 500;
        body = "{}";

        assertThrows(UnauthorizedException.class, () -> directory.requireSession("Bearer good"));
    }

    // ------------------------------------------------------------------ requireAdmin

    @Test
    void requireAdminAcceptsAnAdmin() {
        status = 200;
        body = sessionJson("Admin");

        assertEquals(USER_ID, directory.requireAdmin("Bearer good").getUserId());
    }

    @Test
    void requireAdminAcceptsASuperAdmin() {
        status = 200;
        body = sessionJson("SUPERADMIN");

        assertEquals(USER_ID, directory.requireAdmin("Bearer good").getUserId());
    }

    @Test
    void requireAdminRejectsAMember() {
        status = 200;
        body = sessionJson("user");

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> directory.requireAdmin("Bearer good"));

        assertEquals("Admin access is required", exception.getMessage());
    }

    @Test
    void requireAdminRejectsASessionWithoutARole() {
        status = 200;
        body = sessionJson(null);

        assertThrows(ForbiddenException.class, () -> directory.requireAdmin("Bearer good"));
    }

    @Test
    void requireAdminRejectsAMissingToken() {
        assertThrows(UnauthorizedException.class, () -> directory.requireAdmin(null));
    }

    // ------------------------------------------------------------------ listUsers

    @Test
    void listUsersFiltersByRoleAndSendsTheInternalToken() {
        status = 200;
        body = "[" + userJson() + "]";

        List<UserCard> users = directory.listUsers("user");

        assertEquals(1, users.size());
        assertEquals(USER_ID, users.get(0).getId());
        assertEquals("Asha Rao", users.get(0).getFullName());
        assertEquals("/api/users", seenPath.get());
        assertEquals("role=user", seenQuery.get());
        assertEquals("internal-secret", seenToken.get());
    }

    @Test
    void listUsersWithoutARoleAsksForEveryone() {
        status = 200;
        body = "[]";

        assertTrue(directory.listUsers(null).isEmpty());
        assertNull(seenQuery.get());
    }

    @Test
    void listUsersWithABlankRoleAsksForEveryone() {
        status = 200;
        body = "[]";

        assertTrue(directory.listUsers("  ").isEmpty());
        assertNull(seenQuery.get());
    }

    @Test
    void listUsersWithAnEmptyBodyGivesAnEmptyList() {
        status = 200;
        body = null;

        assertTrue(directory.listUsers("admin").isEmpty());
    }

    @Test
    void listUsersFailureBecomesAValidationError() {
        status = 500;
        body = "{}";

        ValidationException exception = assertThrows(ValidationException.class, () -> directory.listUsers("user"));
        assertEquals("Could not load users", exception.getMessage());
    }

    // ------------------------------------------------------------------ createUser

    @Test
    void createUserPostsTheRequestAndReturnsTheCreatedUser() {
        status = 201;
        body = "{\"ok\":true,\"data\":" + userJson() + "}";
        UserWriteRequest request = new UserWriteRequest();
        request.setFullName("Asha Rao");
        request.setEmail("asha@example.com");

        UserCard created = directory.createUser(request);

        assertEquals(USER_ID, created.getId());
        assertEquals("POST", seenMethod.get());
        assertEquals("/api/users", seenPath.get());
        assertEquals("internal-secret", seenToken.get());
        assertTrue(seenBody.get().contains("\"email\":\"asha@example.com\""));
    }

    @Test
    void createUserWithAnEnvelopeWithoutDataFails() {
        status = 200;
        body = "{\"ok\":true}";

        ValidationException exception =
                assertThrows(ValidationException.class, () -> directory.createUser(new UserWriteRequest()));
        assertEquals("Could not create the user", exception.getMessage());
    }

    @Test
    void createUserWithAnEmptyResponseFails() {
        status = 200;
        body = null;

        assertThrows(ValidationException.class, () -> directory.createUser(new UserWriteRequest()));
    }

    @Test
    void createUserWithADuplicateEmailIsReported() {
        status = 409;
        body = "{}";

        ValidationException exception =
                assertThrows(ValidationException.class, () -> directory.createUser(new UserWriteRequest()));
        assertEquals("Email is already registered", exception.getMessage());
    }

    @Test
    void createUserNotFoundIsReported() {
        status = 404;
        body = "{}";

        assertThrows(ResourceNotFoundException.class, () -> directory.createUser(new UserWriteRequest()));
    }

    @Test
    void createUserOtherFailuresUseTheFallbackMessage() {
        status = 500;
        body = "{}";

        ValidationException exception =
                assertThrows(ValidationException.class, () -> directory.createUser(new UserWriteRequest()));
        assertEquals("Could not create the user", exception.getMessage());
    }

    // ------------------------------------------------------------------ updateUser

    @Test
    void updateUserPutsTheRequestAndReturnsTheUpdatedUser() {
        status = 200;
        body = "{\"data\":" + userJson() + "}";
        UserWriteRequest request = new UserWriteRequest();
        request.setStatus("inactive");

        UserCard updated = directory.updateUser(USER_ID, request);

        assertEquals(USER_ID, updated.getId());
        assertEquals("PUT", seenMethod.get());
        assertEquals("/api/users/" + USER_ID, seenPath.get());
        assertEquals("internal-secret", seenToken.get());
        assertTrue(seenBody.get().contains("\"status\":\"inactive\""));
    }

    @Test
    void updateUserWithAnEmptyEnvelopeIsNotFound() {
        status = 200;
        body = "{}";

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> directory.updateUser(USER_ID, new UserWriteRequest()));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void updateUserWithAnEmptyResponseIsNotFound() {
        status = 200;
        body = null;

        assertThrows(ResourceNotFoundException.class, () -> directory.updateUser(USER_ID, new UserWriteRequest()));
    }

    @Test
    void updateUserNotFoundStatusIsMapped() {
        status = 404;
        body = "{}";

        assertThrows(ResourceNotFoundException.class, () -> directory.updateUser(USER_ID, new UserWriteRequest()));
    }

    @Test
    void updateUserDuplicateEmailIsMapped() {
        status = 409;
        body = "{}";

        ValidationException exception = assertThrows(ValidationException.class,
                () -> directory.updateUser(USER_ID, new UserWriteRequest()));
        assertEquals("Email is already registered", exception.getMessage());
    }

    @Test
    void updateUserOtherFailuresUseTheFallbackMessage() {
        status = 503;
        body = "{}";

        ValidationException exception = assertThrows(ValidationException.class,
                () -> directory.updateUser(USER_ID, new UserWriteRequest()));
        assertEquals("Could not update the user", exception.getMessage());
    }

    // ------------------------------------------------------------------ deleteUser

    @Test
    void deleteUserSendsADeleteWithTheInternalToken() {
        status = 204;
        body = null;

        directory.deleteUser(USER_ID);

        assertEquals("DELETE", seenMethod.get());
        assertEquals("/api/users/" + USER_ID, seenPath.get());
        assertEquals("internal-secret", seenToken.get());
    }

    @Test
    void deleteUserNotFoundIsMapped() {
        status = 404;
        body = "{}";

        assertThrows(ResourceNotFoundException.class, () -> directory.deleteUser(USER_ID));
    }

    @Test
    void deleteUserConflictIsMapped() {
        status = 409;
        body = "{}";

        assertThrows(ValidationException.class, () -> directory.deleteUser(USER_ID));
    }

    @Test
    void deleteUserOtherFailuresUseTheFallbackMessage() {
        status = 500;
        body = "{}";

        ValidationException exception = assertThrows(ValidationException.class, () -> directory.deleteUser(USER_ID));
        assertEquals("Could not delete the user", exception.getMessage());
    }
}
