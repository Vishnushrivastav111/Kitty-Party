package com.microvault.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.microvault.dashboard.exception.ResourceNotFoundException;
import com.microvault.dashboard.exception.UnauthorizedException;
import com.microvault.dashboard.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AuthClient {

    private final RestClient restClient;

    public AuthClient(@Value("${auth-service.url}") String authServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(authServiceUrl).build();
    }

    public JsonNode currentUser(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new UnauthorizedException("Login is required");
        }
        try {
            JsonNode user = restClient.get()
                    .uri("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(JsonNode.class);
            if (user == null || user.isNull() || user.path("id").isMissingNode()) {
                throw new UnauthorizedException("Login is required");
            }
            return user;
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                throw new UnauthorizedException("Login is required");
            }
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("No user found for this dashboard");
            }
            throw new ValidationException("Could not load the user");
        }
    }
}
