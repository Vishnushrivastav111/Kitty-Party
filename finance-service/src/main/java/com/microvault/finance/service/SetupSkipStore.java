package com.microvault.finance.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SetupSkipStore {

    private final Set<UUID> skippedUserIds = ConcurrentHashMap.newKeySet();

    public void skip(UUID userId) {
        skippedUserIds.add(userId);
    }

    public void clear(UUID userId) {
        skippedUserIds.remove(userId);
    }

    public boolean isSkipped(UUID userId) {
        return skippedUserIds.contains(userId);
    }
}
