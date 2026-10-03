package com.microvault.finance.client;

import com.microvault.finance.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class AuthClient {

    private final RestClient restClient;

    public AuthClient(@Value("${auth-service.url}") String authServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(authServiceUrl).build();
    }

    public UUID requireUserId(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new UnauthorizedException("Login is required");
        }
        try {
            SessionBody session = restClient.get()
                    .uri("/api/auth/session")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                        throw new UnauthorizedException("Login is required");
                    })
                    .body(SessionBody.class);
            if (session == null || session.userId == null) {
                throw new UnauthorizedException("Login is required");
            }
            return session.userId;
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new UnauthorizedException("Login is required");
        }
    }

    public static class SessionBody {
        public UUID userId;
    }
}
