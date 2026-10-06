package com.microvault.finance.service;

import com.microvault.finance.client.AuthClient;
import com.microvault.finance.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestUserTest {

    @Mock
    private AuthClient authClient;

    @Test
    void requireUserDelegatesToAuthClient() {
        UUID userId = UUID.randomUUID();
        when(authClient.requireUserId("Bearer abc")).thenReturn(userId);

        assertEquals(userId, new RequestUser(authClient, "secret").requireUser("Bearer abc"));
    }

    @Test
    void requireUserPropagatesAuthFailure() {
        when(authClient.requireUserId("Bearer bad")).thenThrow(new UnauthorizedException("Login is required"));
        RequestUser requestUser = new RequestUser(authClient, "secret");

        assertThrows(UnauthorizedException.class, () -> requestUser.requireUser("Bearer bad"));
    }

    @Test
    void requireInternalAcceptsTheConfiguredToken() {
        RequestUser requestUser = new RequestUser(authClient, "secret");

        assertDoesNotThrow(() -> requestUser.requireInternal("secret"));
    }

    @Test
    void requireInternalRejectsAWrongToken() {
        RequestUser requestUser = new RequestUser(authClient, "secret");

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> requestUser.requireInternal("nope"));

        assertEquals("Login is required", exception.getMessage());
    }

    @Test
    void requireInternalRejectsAMissingToken() {
        RequestUser requestUser = new RequestUser(authClient, "secret");

        assertThrows(UnauthorizedException.class, () -> requestUser.requireInternal(null));
    }
}
