package net.uebliche.hub.common.storage;

import java.util.UUID;

public interface PlayerLocationRepository extends AutoCloseable {
    PlayerLocation getLocation(String serverId, UUID playerId) throws StorageException;

    void saveLocation(PlayerLocation location) throws StorageException;

    @Override
    void close();
}
