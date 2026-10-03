package com.microvault.auth.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenStoreTest {

    private final TokenStore tokenStore = new TokenStore("microvault-local-jwt-secret-change-me-32b", 12);

    @Test
    void issuesASignedJwtAndReadsTheUserId() {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        String token = tokenStore.issue(userId);

        assertTrue(token.chars().filter(ch -> ch == '.').count() == 2);
        assertEquals(userId, tokenStore.requireUserId("Bearer " + token));
    }

    @Test
    void rejectsATamperedOrLoggedOutToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenStore.issue(userId);

        tokenStore.revoke("Bearer " + token);
        assertNull(tokenStore.requireUserId("Bearer " + token));

        String tampered = token.substring(0, token.length() - 2) + "aa";
        assertNull(tokenStore.requireUserId(tampered));
        assertNotNull(token);
    }
}
