package net.uebliche.hub.fabric;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.uebliche.hub.common.model.LobbyListEntry;

public class LobbyCacheService {
    private final Map<UUID, List<LobbyListEntry>> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cacheAge = new ConcurrentHashMap<>();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final long cacheTtlMillis;

    public LobbyCacheService(long cacheTtlMillis) {
        this.cacheTtlMillis = cacheTtlMillis;
    }

    public void markPending(UUID playerId) {
        pending.add(playerId);
    }

    public boolean consumePending(UUID playerId) {
        return pending.remove(playerId);
    }

    public boolean hasFreshCache(UUID playerId) {
        Long age = cacheAge.get(playerId);
        if (age == null) {
            return false;
        }
        return System.currentTimeMillis() - age < cacheTtlMillis;
    }

    public void store(UUID playerId, List<LobbyListEntry> entries) {
        cache.put(playerId, entries);
        cacheAge.put(playerId, System.currentTimeMillis());
    }

    public List<LobbyListEntry> entriesFor(UUID playerId) {
        return cache.getOrDefault(playerId, List.of());
    }
}
