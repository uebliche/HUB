package net.uebliche.hub.common.storage;

import java.util.UUID;

public final class FallbackPlayerLocationRepository implements PlayerLocationRepository {
    private final PlayerLocationRepository primary;
    private final PlayerLocationRepository fallback;
    private final StorageLogger logger;

    public FallbackPlayerLocationRepository(PlayerLocationRepository primary, PlayerLocationRepository fallback, StorageLogger logger) {
        this.primary = primary;
        this.fallback = fallback;
        this.logger = logger == null ? StorageLogger.noop() : logger;
    }

    @Override
    public PlayerLocation getLocation(String serverId, UUID playerId) {
        try {
            return primary.getLocation(serverId, playerId);
        } catch (Exception ex) {
            logger.warn("Primary location storage failed, using fallback: " + ex.getMessage());
            return fallback.getLocation(serverId, playerId);
        }
    }

    @Override
    public void saveLocation(PlayerLocation location) {
        try {
            primary.saveLocation(location);
        } catch (Exception ex) {
            logger.warn("Primary location storage failed, using fallback: " + ex.getMessage());
            fallback.saveLocation(location);
        }
    }

    @Override
    public void close() {
        try {
            primary.close();
        } catch (Exception ex) {
            logger.warn("Failed to close primary location storage: " + ex.getMessage());
        }
        try {
            fallback.close();
        } catch (Exception ex) {
            logger.warn("Failed to close fallback location storage: " + ex.getMessage());
        }
    }
}
