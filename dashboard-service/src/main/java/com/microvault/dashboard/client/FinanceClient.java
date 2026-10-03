package com.microvault.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class FinanceClient {

    private final RestClient restClient;
    private final String internalToken;

    public FinanceClient(@Value("${finance-service.url}") String financeServiceUrl,
                         @Value("${microvault.internal.token}") String internalToken) {
        this.restClient = RestClient.builder().baseUrl(financeServiceUrl).build();
        this.internalToken = internalToken;
    }

    public JsonNode workspace(UUID userId) {
        return restClient.get()
                .uri("/api/finance/workspace?userId={userId}", userId)
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .body(JsonNode.class);
    }
}
