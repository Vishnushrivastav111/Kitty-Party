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
        List<UUID> ids = restClient.get().uri("/api/finance/profiles/active-user-ids")
                .header("X-Internal-Token", internalToken).retrieve()
                .body(new ParameterizedTypeReference<List<UUID>>() {});
        return ids == null ? List.of() : ids;
    }

    /** The transaction an approval request is about, or null when it cannot be found or loaded. */
    public TransactionSummary transaction(UUID userId, UUID transactionId) {
        try {
            return restClient.get()
                    .uri(builder -> builder.path("/api/internal/transactions/{id}").queryParam("userId", userId).build(transactionId))
                    .header("X-Internal-Token", internalToken)
                    .retrieve().body(TransactionSummary.class);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public static class TransactionSummary {
        private String name;
        private String category;
        private String type;
        private java.math.BigDecimal amount;
        private String date;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
    }

    public void softDeleteTransaction(UUID userId, UUID transactionId) {
        restClient.delete()
                .uri(builder -> builder.path("/api/internal/transactions/{id}").queryParam("userId", userId).build(transactionId))
                .header("X-Internal-Token", internalToken)
                .retrieve().toBodilessEntity();
    }
}
