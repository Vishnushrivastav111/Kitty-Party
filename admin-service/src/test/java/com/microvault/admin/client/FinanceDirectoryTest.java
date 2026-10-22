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
    void describesTheTransactionBehindAnApprovalRequest() {
        UUID user = UUID.randomUUID();
        UUID transaction = UUID.randomUUID();
        status = 200;
        body = "{\"name\":\"Laptop bag\",\"category\":\"Shopping\",\"type\":\"expense\",\"amount\":2499,\"date\":\"2026-10-16\"}";

        FinanceDirectory.TransactionSummary summary = directory.transaction(user, transaction);

        assertEquals("Laptop bag", summary.getName());
        assertEquals("Shopping", summary.getCategory());
        assertEquals(0, new java.math.BigDecimal("2499").compareTo(summary.getAmount()));
        assertEquals("/api/internal/transactions/" + transaction, seenPath.get());
        assertEquals("internal-secret", seenToken.get());
    }

    @Test
    void aTransactionThatCannotBeLoadedIsNullNotAnError() {
        status = 404;
        body = "{}";

        assertEquals(null, directory.transaction(UUID.randomUUID(), UUID.randomUUID()));
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
    void softDeleteAsksFinanceToRemoveTheTransaction() {
        status = 204;
        body = null;
        UUID user = UUID.randomUUID();
        UUID transaction = UUID.randomUUID();

        directory.softDeleteTransaction(user, transaction);

        assertEquals("/api/internal/transactions/" + transaction, seenPath.get());
        assertEquals("internal-secret", seenToken.get());
    }

    @Test
    void aTransactionSummaryRoundTripsEveryField() {
        FinanceDirectory.TransactionSummary summary = new FinanceDirectory.TransactionSummary();
        summary.setName("Tea");
        summary.setCategory("Food");
        summary.setType("expense");
        summary.setAmount(new java.math.BigDecimal("10.00"));
        summary.setDate("2026-10-01");

        assertEquals("Tea", summary.getName());
        assertEquals("Food", summary.getCategory());
        assertEquals("expense", summary.getType());
        assertEquals(0, new java.math.BigDecimal("10.00").compareTo(summary.getAmount()));
        assertEquals("2026-10-01", summary.getDate());
    }

    @Test
    void aRejectedInternalTokenIsPropagated() {
        status = 401;
        body = "{}";

        assertThrows(RestClientResponseException.class, () -> directory.usersWithProfile());
    }
}
