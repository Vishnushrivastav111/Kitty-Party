package com.microvault.finance.client;

import com.microvault.finance.exception.UnauthorizedException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
            exchange.sendResponseHeaders(status, status == 204 ? -1 : payload.length);
            if (status != 204) {
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
    void returnsTheUserIdOfAValidSessionAndForwardsTheToken() {
        UUID userId = UUID.randomUUID();
        status = 200;
        body = "{\"userId\":\"" + userId + "\"}";

        assertEquals(userId, client.requireUserId("Bearer valid-token"));
        assertEquals("Bearer valid-token", seenAuthorization.get());
        assertEquals("/api/auth/session", seenPath.get());
    }

    @Test
    void ignoresUnknownFieldsInTheSessionResponse() {
        UUID userId = UUID.randomUUID();
        status = 200;
        body = "{\"userId\":\"" + userId + "\",\"role\":\"USER\",\"name\":\"Asha\"}";

        assertEquals(userId, client.requireUserId("Bearer valid-token"));
    }

    @Test
    void missingAuthorizationHeaderIsRejectedWithoutCallingTheServer() {
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> client.requireUserId(null));

        assertEquals("Login is required", exception.getMessage());
        assertEquals(null, seenPath.get());
    }

    @Test
    void blankAuthorizationHeaderIsRejectedWithoutCallingTheServer() {
        assertThrows(UnauthorizedException.class, () -> client.requireUserId("   "));
        assertEquals(null, seenPath.get());
    }

    @Test
    void invalidOrExpiredTokenAnswered401IsRejected() {
        status = 401;
        body = "{\"message\":\"Session expired\"}";

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> client.requireUserId("Bearer expired"));

        assertEquals("Login is required", exception.getMessage());
    }

    @Test
    void forbiddenAndNotFoundAnswersAreRejected() {
        status = 403;
        body = "{}";
        assertThrows(UnauthorizedException.class, () -> client.requireUserId("Bearer x"));

        status = 404;
        assertThrows(UnauthorizedException.class, () -> client.requireUserId("Bearer x"));
    }

    @Test
    void serverErrorFromTheAuthServiceIsRejected() {
        status = 500;
        body = "{\"message\":\"boom\"}";

        assertThrows(UnauthorizedException.class, () -> client.requireUserId("Bearer x"));
    }

    @Test
    void sessionWithoutUserIdIsRejected() {
        status = 200;
        body = "{}";

        assertThrows(UnauthorizedException.class, () -> client.requireUserId("Bearer x"));
    }

    @Test
    void emptyResponseBodyIsRejected() {
        status = 204;
        body = null;

        assertThrows(UnauthorizedException.class, () -> client.requireUserId("Bearer x"));
    }

    @Test
    void malformedResponseBodyIsRejected() {
        status = 200;
        body = "this is not json";

        assertThrows(UnauthorizedException.class, () -> client.requireUserId("Bearer x"));
    }

    @Test
    void unreachableAuthServiceIsRejected() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        AuthClient unreachable = new AuthClient("http://127.0.0.1:" + closedPort);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> unreachable.requireUserId("Bearer x"));

        assertEquals("Login is required", exception.getMessage());
    }

    @Test
    void sessionBodyHoldsTheUserId() {
        AuthClient.SessionBody session = new AuthClient.SessionBody();
        UUID userId = UUID.randomUUID();
        session.userId = userId;

        assertNotNull(session);
        assertEquals(userId, session.userId);
    }
}
