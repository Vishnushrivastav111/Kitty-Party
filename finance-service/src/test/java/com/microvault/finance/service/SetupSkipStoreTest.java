package com.microvault.finance.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupSkipStoreTest {

    private final SetupSkipStore store = new SetupSkipStore();

    @Test
    void unknownUserIsNotSkipped() {
        assertFalse(store.isSkipped(UUID.randomUUID()));
    }

    @Test
    void skippedUserIsRemembered() {
        UUID userId = UUID.randomUUID();

        store.skip(userId);

        assertTrue(store.isSkipped(userId));
        assertFalse(store.isSkipped(UUID.randomUUID()));
    }

    @Test
    void clearForgetsTheUser() {
        UUID userId = UUID.randomUUID();
        store.skip(userId);

        store.clear(userId);

        assertFalse(store.isSkipped(userId));
    }

    @Test
    void clearOfUnknownUserIsHarmless() {
        UUID userId = UUID.randomUUID();

        store.clear(userId);

        assertFalse(store.isSkipped(userId));
    }
}
