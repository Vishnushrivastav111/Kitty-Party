package com.microvault.finance.service;

import com.microvault.finance.client.AuthClient;
import com.microvault.finance.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequestUser {

    private final AuthClient authClient;
    private final String internalToken;

    public RequestUser(AuthClient authClient,
                       @Value("${microvault.internal.token}") String internalToken) {
        this.authClient = authClient;
        this.internalToken = internalToken;
    }

    public UUID requireUser(String authorization) {
        return authClient.requireUserId(authorization);
    }

    public void requireInternal(String token) {
        if (token == null || !token.equals(internalToken)) {
            throw new UnauthorizedException("Login is required");
        }
    }
}
