package com.microvault.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** AdminClient and FinanceClient against a real in-process HTTP server. */
class WorkspaceClientsTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> seenToken = new AtomicReference<>();
    private final AtomicReference<String> seenPath = new AtomicReference<>();
    private final AtomicReference<String> seenQuery = new AtomicReference<>();
    private volatile int status;
    private volatile String body;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            seenToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            seenPath.set(exchange.getRequestURI().getPath());
            seenQuery.set(exchange.getRequestURI().getQuery());
            byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, payload.length == 0 ? -1 : payload.length);
            if (payload.length > 0) {
                exchange.getResponseBody().write(payload);
            }
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void adminClientSendsTheUserRoleAndInternalToken() {
        status = 200;
        body = "{\"notifications\":[]}";

        JsonNode workspace = new AdminClient(baseUrl, "secret").workspace(USER_ID, "admin");

        assertEquals(true, workspace.has("notifications"));
        assertEquals("/api/admin/workspace", seenPath.get());
        assertEquals("userId=" + USER_ID + "&role=admin", seenQuery.get());
        assertEquals("secret", seenToken.get());
    }

    @Test
    void adminClientDefaultsTheRoleToUser() {
        status = 200;
        body = "{}";

        new AdminClient(baseUrl, "secret").workspace(USER_ID, null);

        assertEquals("userId=" + USER_ID + "&role=user", seenQuery.get());
    }

    @Test
    void adminClientReturnsNullForAnEmptyBody() {
        status = 200;
        body = null;

        assertNull(new AdminClient(baseUrl, "secret").workspace(USER_ID, "user"));
    }

    @Test
    void adminClientPropagatesAnErrorStatus() {
        status = 500;
        body = "{}";
        AdminClient client = new AdminClient(baseUrl, "secret");

        assertThrows(RestClientResponseException.class, () -> client.workspace(USER_ID, "user"));
    }

    @Test
    void financeClientSendsTheUserIdAndInternalToken() {
        status = 200;
        body = "{\"goals\":[]}";

        JsonNode workspace = new FinanceClient(baseUrl, "secret").workspace(USER_ID);

        assertEquals(true, workspace.has("goals"));
        assertEquals("/api/finance/workspace", seenPath.get());
        assertEquals("userId=" + USER_ID, seenQuery.get());
        assertEquals("secret", seenToken.get());
    }

    @Test
    void financeClientReturnsNullForAnEmptyBody() {
        status = 200;
        body = null;

        assertNull(new FinanceClient(baseUrl, "secret").workspace(USER_ID));
    }

    @Test
    void financeClientPropagatesAnErrorStatus() {
        status = 503;
        body = "{}";
        FinanceClient client = new FinanceClient(baseUrl, "secret");

        assertThrows(RestClientResponseException.class, () -> client.workspace(USER_ID));
    }
}
