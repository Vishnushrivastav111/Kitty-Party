package com.microvault.admin.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class FinanceDirectory {

    private final RestClient restClient;
    private final String internalToken;

    public FinanceDirectory(@Value("${finance-service.url}") String financeServiceUrl,
                            @Value("${microvault.internal.token}") String internalToken) {
        this.restClient = RestClient.builder().baseUrl(financeServiceUrl).build();
        this.internalToken = internalToken;
    }

    public List<UUID> usersWithProfile() {
        List<UUID> ids = restClient.get()
                .uri("/api/finance/profiles/active-user-ids")
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UUID>>() {
                });
        return ids == null ? List.of() : ids;
    }
}
