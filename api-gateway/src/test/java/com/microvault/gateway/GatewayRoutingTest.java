package com.microvault.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Starts the real gateway on a random port in front of four stub services and checks which service each
 * public path is forwarded to.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final List<HttpServer> SERVERS = new ArrayList<>();

    @Autowired
    private WebTestClient client;

    @Autowired
    private RouteLocator routeLocator;

    private static final String AUTH_URL = start("auth");
    private static final String FINANCE_URL = start("finance");
    private static final String DASHBOARD_URL = start("dashboard");
    private static final String ADMIN_URL = start("admin");

    @DynamicPropertySource
    static void backends(DynamicPropertyRegistry registry) {
        registry.add("auth-service.url", () -> AUTH_URL);
        registry.add("finance-service.url", () -> FINANCE_URL);
        registry.add("dashboard-service.url", () -> DASHBOARD_URL);
        registry.add("admin-service.url", () -> ADMIN_URL);
    }

    private static String start(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", exchange -> {
                byte[] payload = (name + ":" + exchange.getRequestMethod() + ":" + exchange.getRequestURI().getPath())
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, payload.length);
                exchange.getResponseBody().write(payload);
                exchange.close();
            });
            server.start();
            SERVERS.add(server);
            return "http://127.0.0.1:" + server.getAddress().getPort();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @AfterAll
    static void stopBackends() {
        SERVERS.forEach(server -> server.stop(0));
    }

    @Test
    void allFourServicesAreRegisteredAsRoutes() {
        List<String> ids = routeLocator.getRoutes().map(route -> route.getId()).collectList().block();

        assertEquals(List.of("auth-service", "finance-service", "dashboard-service", "admin-service"), ids);
    }

    @ParameterizedTest(name = "{0} is forwarded to the {1} service")
    @CsvSource({
            "/api/auth/login, auth",
            "/api/auth/session, auth",
            "/api/users/me, auth",
            "/api/finance, finance",
            "/api/finance/workspace, finance",
            "/api/transactions, finance",
            "/api/transactions/abc, finance",
            "/api/budgets, finance",
            "/api/goals/abc, finance",
            "/api/savings, finance",
            "/api/reports, finance",
            "/api/affordability, finance",
            "/api/affordability/history, finance",
            "/api/dashboard, dashboard",
            "/api/admin, admin",
            "/api/admin/workspace, admin",
            "/api/feedback, admin",
            "/api/feedback/abc/history, admin",
            "/api/notifications, admin",
            "/api/notifications/read-all, admin"
    })
    void publicPathsAreForwardedToTheOwningService(String path, String service) {
        client.get().uri(path).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(service + ":GET:" + path);
    }

    @Test
    void theHttpMethodIsPreserved() {
        client.post().uri("/api/auth/login").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("auth:POST:/api/auth/login");
        client.delete().uri("/api/notifications/abc").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("admin:DELETE:/api/notifications/abc");
    }

    @Test
    void theDashboardRouteDoesNotCoverSubPaths() {
        client.get().uri("/api/dashboard/other").exchange().expectStatus().isNotFound();
    }

    @Test
    void unknownPathsAreNotFound() {
        client.get().uri("/api/unknown").exchange().expectStatus().isNotFound();
        client.get().uri("/").exchange().expectStatus().isNotFound();
    }

    @Test
    void corsPreflightIsAnsweredForAnyOrigin() {
        client.options().uri("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
    }
}
