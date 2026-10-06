package com.microvault.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenStoreTest {

    private static final String SECRET = "microvault-local-jwt-secret-change-me-32b";

    private final TokenStore tokenStore = new TokenStore(SECRET, 12);
    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void issuesASignedJwtAndReadsTheUserIdBack() {
        String token = tokenStore.issue(userId);

        assertEquals(2, token.chars().filter(ch -> ch == '.').count());
        assertEquals(userId, tokenStore.requireUserId("Bearer " + token));
    }

    @Test
    void everyIssuedTokenIsUnique() {
        assertNotEquals(tokenStore.issue(userId), tokenStore.issue(userId));
    }

    @Test
    void acceptsTheBearerPrefixInAnyCaseAndWithSurroundingWhitespace() {
        String token = tokenStore.issue(userId);

        assertEquals(userId, tokenStore.requireUserId("bearer " + token));
        assertEquals(userId, tokenStore.requireUserId("  BEARER   " + token + "  "));
    }

    @Test
    void acceptsABareTokenWithoutThePrefix() {
        String token = tokenStore.issue(userId);

        assertEquals(userId, tokenStore.requireUserId(token));
    }

    @Test
    void rejectsAMissingToken() {
        assertNull(tokenStore.requireUserId(null));
    }

    @Test
    void rejectsABlankOrPrefixOnlyToken() {
        assertNull(tokenStore.requireUserId(""));
        assertNull(tokenStore.requireUserId("   "));
        assertNull(tokenStore.requireUserId("Bearer "));
        assertNull(tokenStore.requireUserId("Bearer    "));
    }

    @Test
    void rejectsAMalformedToken() {
        assertNull(tokenStore.requireUserId("Bearer not-a-jwt"));
        assertNull(tokenStore.requireUserId("Bearer aaa.bbb.ccc"));
        assertNull(tokenStore.requireUserId("garbage"));
    }

    @Test
    void rejectsATamperedToken() {
        String token = tokenStore.issue(userId);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("aa") ? "bb" : "aa");

        assertNull(tokenStore.requireUserId("Bearer " + tampered));
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        TokenStore other = new TokenStore("another-secret-another-secret-another-secret", 12);

        assertNull(tokenStore.requireUserId("Bearer " + other.issue(userId)));
    }

    @Test
    void rejectsAnExpiredToken() {
        TokenStore alreadyExpired = new TokenStore(SECRET, -1);

        assertNull(alreadyExpired.requireUserId("Bearer " + alreadyExpired.issue(userId)));
    }

    @Test
    void rejectsAValidlySignedTokenThatWasIssuedInThePastAndExpired() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(7200);
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plusSeconds(60)))
                .signWith(key)
                .compact();

        assertNull(tokenStore.requireUserId("Bearer " + token));
    }

    @Test
    void rejectsATokenWithoutAnId() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(userId.toString())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(key)
                .compact();

        assertNull(tokenStore.requireUserId("Bearer " + token));
    }

    @Test
    void rejectsATokenWhoseSubjectIsNotAUuid() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("not-a-uuid")
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(key)
                .compact();

        assertNull(tokenStore.requireUserId("Bearer " + token));
    }

    @Test
    void rejectsALoggedOutToken() {
        String token = tokenStore.issue(userId);

        tokenStore.revoke("Bearer " + token);

        assertNull(tokenStore.requireUserId("Bearer " + token));
    }

    @Test
    void loggingOutOneTokenDoesNotAffectAnother() {
        String first = tokenStore.issue(userId);
        String second = tokenStore.issue(userId);

        tokenStore.revoke(first);

        assertNull(tokenStore.requireUserId(first));
        assertEquals(userId, tokenStore.requireUserId(second));
    }

    @Test
    void revokingAnInvalidOrMissingTokenIsHarmless() {
        tokenStore.revoke(null);
        tokenStore.revoke("Bearer garbage");
        tokenStore.revoke("");

        assertTrue(tokenStore.issue(userId).length() > 20);
    }

    @Test
    void revokingATokenWithoutAnIdIsHarmless() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String noId = Jwts.builder().subject(userId.toString()).signWith(key).compact();

        tokenStore.revoke(noId);

        assertNull(tokenStore.requireUserId(noId));
    }

    @Test
    void refusesAJwtSecretThatIsTooShort() {
        assertThrows(WeakKeyException.class, () -> new TokenStore("short", 12));
    }
}
