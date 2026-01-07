package net.uebliche.hub.common.storage;

import java.util.UUID;

public final class CachedPlayerLocationRepository implements PlayerLocationRepository {
    private final PlayerLocationRepository cache;
    private final PlayerLocationRepository primary;
    private final StorageLogger logger;

    public CachedPlayerLocationRepository(PlayerLocationRepository cache, PlayerLocationRepository primary, StorageLogger logger) {
        this.cache = cache;
        this.primary = primary;
        this.logger = logger == null ? StorageLogger.noop() : logger;
    }

    @Override
    public PlayerLocation getLocation(String serverId, UUID playerId) {
        try {
            PlayerLocation cached = cache.getLocation(serverId, playerId);
            if (cached != null) {
                return cached;
            }
        } catch (Exception ex) {
            logger.warn("Location cache read failed: " + ex.getMessage());
        }

        PlayerLocation loaded = primary.getLocation(serverId, playerId);
        if (loaded != null) {
            try {
                cache.saveLocation(loaded);
            } catch (Exception ex) {
                logger.warn("Location cache write failed: " + ex.getMessage());
            }
        }
        return loaded;
    }

    @Override
    public void saveLocation(PlayerLocation location) {
        primary.saveLocation(location);
        try {
            cache.saveLocation(location);
        } catch (Exception ex) {
            logger.warn("Location cache write failed: " + ex.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            cache.close();
        } catch (Exception ex) {
            logger.warn("Failed to close location cache: " + ex.getMessage());
        }
        try {
            primary.close();
        } catch (Exception ex) {
            logger.warn("Failed to close primary location storage: " + ex.getMessage());
        }
    }
}
