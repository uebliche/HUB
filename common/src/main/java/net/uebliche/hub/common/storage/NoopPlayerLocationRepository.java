package net.uebliche.hub.common.storage;

import java.util.UUID;

public final class NoopPlayerLocationRepository implements PlayerLocationRepository {
    @Override
    public PlayerLocation getLocation(String serverId, UUID playerId) {
        return null;
    }

    @Override
    public void saveLocation(PlayerLocation location) {
    }

    @Override
    public void close() {
    }
}
