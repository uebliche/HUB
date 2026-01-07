package net.uebliche.hub.common.storage;

public final class StorageManager implements AutoCloseable {
    private final StorageSettings settings;
    private final JumpRunScoreRepository scores;
    private final PlayerLocationRepository locations;

    private StorageManager(StorageSettings settings, JumpRunScoreRepository scores, PlayerLocationRepository locations) {
        this.settings = settings;
        this.scores = scores;
        this.locations = locations;
    }

    public static StorageManager create(StorageSettings settings, StorageLogger logger) {
        StorageLogger safeLogger = logger == null ? StorageLogger.noop() : logger;
        StorageSettings safeSettings = settings == null ? new StorageSettings("lobby", null, null, null, null, null, null, true) : settings;

        JumpRunScoreRepository scorePrimary = createScoreRepository(safeSettings.scores().primary(), safeSettings, safeLogger);
        if (scorePrimary == null) {
            scorePrimary = new NoopJumpRunScoreRepository();
        }
        JumpRunScoreRepository scoreRepo = applyFallback(scorePrimary, safeSettings, safeLogger);

        PlayerLocationRepository locationPrimary = createLocationRepository(safeSettings.locations().primary(), safeSettings, safeLogger);
        if (locationPrimary == null) {
            locationPrimary = new NoopPlayerLocationRepository();
        }
        PlayerLocationRepository locationRepo = applyFallback(locationPrimary, safeSettings, safeLogger);

        StorageBackendType cacheType = safeSettings.locations().cache();
        if (cacheType != null && cacheType != StorageBackendType.NONE && cacheType != safeSettings.locations().primary()) {
            PlayerLocationRepository cacheRepo = createLocationRepository(cacheType, safeSettings, safeLogger);
            if (cacheRepo != null) {
                locationRepo = new CachedPlayerLocationRepository(cacheRepo, locationRepo, safeLogger);
            }
        }

        safeLogger.info("Storage initialized: scores=" + safeSettings.scores().primary() + ", locations=" + safeSettings.locations().primary());
        return new StorageManager(safeSettings, scoreRepo, locationRepo);
    }

    public StorageSettings settings() {
        return settings;
    }

    public JumpRunScoreRepository scores() {
        return scores;
    }

    public PlayerLocationRepository locations() {
        return locations;
    }

    @Override
    public void close() {
        try {
            scores.close();
        } catch (Exception ignored) {
        }
        try {
            locations.close();
        } catch (Exception ignored) {
        }
    }

    private static JumpRunScoreRepository createScoreRepository(StorageBackendType type, StorageSettings settings, StorageLogger logger) {
        try {
            return switch (type) {
                case SQL -> new SqlJumpRunScoreRepository(settings.sql());
                case LOCAL_SQL -> new SqlJumpRunScoreRepository(settings.localSql());
                case MONGO -> new MongoJumpRunScoreRepository(settings.mongo());
                case REDIS -> new RedisJumpRunScoreRepository(settings.redis());
                case NONE -> null;
            };
        } catch (Exception ex) {
            logger.warn("Failed to initialize score storage (" + type + "): " + ex.getMessage());
            return null;
        }
    }

    private static PlayerLocationRepository createLocationRepository(StorageBackendType type, StorageSettings settings, StorageLogger logger) {
        try {
            return switch (type) {
                case SQL -> new SqlPlayerLocationRepository(settings.sql());
                case LOCAL_SQL -> new SqlPlayerLocationRepository(settings.localSql());
                case MONGO -> new MongoPlayerLocationRepository(settings.mongo());
                case REDIS -> new RedisPlayerLocationRepository(settings.redis());
                case NONE -> null;
            };
        } catch (Exception ex) {
            logger.warn("Failed to initialize location storage (" + type + "): " + ex.getMessage());
            return null;
        }
    }

    private static JumpRunScoreRepository applyFallback(JumpRunScoreRepository primary, StorageSettings settings, StorageLogger logger) {
        if (!settings.fallbackEnabled()) {
            return primary;
        }
        if (primary instanceof NoopJumpRunScoreRepository) {
            try {
                return new SqlJumpRunScoreRepository(settings.localSql());
            } catch (Exception ex) {
                logger.warn("Failed to initialize fallback score storage: " + ex.getMessage());
                return primary;
            }
        }
        if (primary instanceof SqlJumpRunScoreRepository && settings.scores().primary().isSql()) {
            return primary;
        }
        try {
            JumpRunScoreRepository fallback = new SqlJumpRunScoreRepository(settings.localSql());
            return new FallbackJumpRunScoreRepository(primary, fallback, logger);
        } catch (Exception ex) {
            logger.warn("Failed to initialize fallback score storage: " + ex.getMessage());
            return primary;
        }
    }

    private static PlayerLocationRepository applyFallback(PlayerLocationRepository primary, StorageSettings settings, StorageLogger logger) {
        if (!settings.fallbackEnabled()) {
            return primary;
        }
        if (primary instanceof NoopPlayerLocationRepository) {
            try {
                return new SqlPlayerLocationRepository(settings.localSql());
            } catch (Exception ex) {
                logger.warn("Failed to initialize fallback location storage: " + ex.getMessage());
                return primary;
            }
        }
        if (primary instanceof SqlPlayerLocationRepository && settings.locations().primary().isSql()) {
            return primary;
        }
        try {
            PlayerLocationRepository fallback = new SqlPlayerLocationRepository(settings.localSql());
            return new FallbackPlayerLocationRepository(primary, fallback, logger);
        } catch (Exception ex) {
            logger.warn("Failed to initialize fallback location storage: " + ex.getMessage());
            return primary;
        }
    }
}
