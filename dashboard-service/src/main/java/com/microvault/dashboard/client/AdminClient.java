package com.microvault.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class AdminClient {

    private final RestClient restClient;
    private final String internalToken;

    public AdminClient(@Value("${admin-service.url}") String adminServiceUrl,
                       @Value("${microvault.internal.token}") String internalToken) {
        this.restClient = RestClient.builder().baseUrl(adminServiceUrl).build();
        this.internalToken = internalToken;
    }

    public JsonNode workspace(UUID userId, String role) {
        return restClient.get()
                .uri(builder -> builder.path("/api/admin/workspace")
                        .queryParam("userId", userId)
                        .queryParam("role", role == null ? "user" : role)
                        .build())
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .body(JsonNode.class);
    }
}
