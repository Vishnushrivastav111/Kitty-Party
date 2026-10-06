package com.microvault.admin.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceDirectoryTest {

    private HttpServer server;
    private FinanceDirectory directory;
    private final AtomicReference<String> seenPath = new AtomicReference<>();
    private final AtomicReference<String> seenToken = new AtomicReference<>();
    private volatile int status;
    private volatile String body;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            seenPath.set(exchange.getRequestURI().getPath());
            seenToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, payload.length == 0 ? -1 : payload.length);
            if (payload.length > 0) {
                exchange.getResponseBody().write(payload);
            }
            exchange.close();
        });
        server.start();
        directory = new FinanceDirectory("http://127.0.0.1:" + server.getAddress().getPort(), "internal-secret");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void returnsTheIdsOfUsersWithAProfile() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        status = 200;
        body = "[\"" + first + "\",\"" + second + "\"]";

        assertEquals(List.of(first, second), directory.usersWithProfile());
        assertEquals("/api/finance/profiles/active-user-ids", seenPath.get());
        assertEquals("internal-secret", seenToken.get());
    }

    @Test
    void anEmptyListStaysEmpty() {
        status = 200;
        body = "[]";

        assertTrue(directory.usersWithProfile().isEmpty());
    }

    @Test
    void anEmptyBodyBecomesAnEmptyList() {
        status = 200;
        body = null;

        assertTrue(directory.usersWithProfile().isEmpty());
    }

    @Test
    void aServerErrorIsPropagated() {
        status = 500;
        body = "{}";

        assertThrows(RestClientResponseException.class, () -> directory.usersWithProfile());
    }

    @Test
    void aRejectedInternalTokenIsPropagated() {
        status = 401;
        body = "{}";

        assertThrows(RestClientResponseException.class, () -> directory.usersWithProfile());
    }
}
