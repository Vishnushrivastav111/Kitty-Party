package com.microvault.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.microvault.dashboard.exception.ResourceNotFoundException;
import com.microvault.dashboard.exception.UnauthorizedException;
import com.microvault.dashboard.exception.ValidationException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Talks to a real in-process HTTP server so the RestClient wiring and error mapping are exercised end to end. */
class AuthClientTest {

    private HttpServer server;
    private AuthClient client;
    private final AtomicReference<String> seenAuthorization = new AtomicReference<>();
    private final AtomicReference<String> seenPath = new AtomicReference<>();
    private volatile int status;
    private volatile String body;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            seenAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            seenPath.set(exchange.getRequestURI().getPath());
            byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, payload.length == 0 ? -1 : payload.length);
            if (payload.length > 0) {
                exchange.getResponseBody().write(payload);
            }
            exchange.close();
        });
        server.start();
        client = new AuthClient("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void returnsTheUserAndForwardsTheToken() {
        status = 200;
        body = "{\"id\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"fullName\":\"Asha\",\"role\":\"user\"}";

        JsonNode user = client.currentUser("Bearer valid-token");

        assertEquals("Asha", user.path("fullName").asText());
        assertEquals("Bearer valid-token", seenAuthorization.get());
        assertEquals("/api/users/me", seenPath.get());
    }

    @Test
    void missingAuthorizationIsRejectedWithoutCallingTheServer() {
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> client.currentUser(null));

        assertEquals("Login is required", exception.getMessage());
        assertNull(seenPath.get());
    }

    @Test
    void blankAuthorizationIsRejectedWithoutCallingTheServer() {
        assertThrows(UnauthorizedException.class, () -> client.currentUser("  "));
        assertNull(seenPath.get());
    }

    @Test
    void emptyResponseBodyIsRejected() {
        status = 200;
        body = null;

        assertThrows(UnauthorizedException.class, () -> client.currentUser("Bearer t"));
    }

    @Test
    void jsonNullResponseIsRejected() {
        status = 200;
        body = "null";

        assertThrows(UnauthorizedException.class, () -> client.currentUser("Bearer t"));
    }

    @Test
    void responseWithoutAnIdIsRejected() {
        status = 200;
        body = "{\"fullName\":\"Asha\"}";

        assertThrows(UnauthorizedException.class, () -> client.currentUser("Bearer t"));
    }

    @Test
    void status401IsRejected() {
        status = 401;
        body = "{}";

        assertThrows(UnauthorizedException.class, () -> client.currentUser("Bearer t"));
    }

    @Test
    void status403IsRejected() {
        status = 403;
        body = "{}";

        assertThrows(UnauthorizedException.class, () -> client.currentUser("Bearer t"));
    }

    @Test
    void status404MeansTheUserDoesNotExist() {
        status = 404;
        body = "{}";

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> client.currentUser("Bearer t"));
        assertEquals("No user found for this dashboard", exception.getMessage());
    }

    @Test
    void anyOtherErrorStatusBecomesAValidationError() {
        status = 500;
        body = "{}";

        ValidationException exception = assertThrows(ValidationException.class, () -> client.currentUser("Bearer t"));
        assertEquals("Could not load the user", exception.getMessage());
    }

    @Test
    void clientErrorOtherThanAuthBecomesAValidationError() {
        status = 400;
        body = "{}";

        assertThrows(ValidationException.class, () -> client.currentUser("Bearer t"));
    }
}
