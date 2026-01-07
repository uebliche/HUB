package net.uebliche.hub.common.storage;

public final class StorageSettings {
    private final String serverId;
    private final ScoreRouting scores;
    private final LocationRouting locations;
    private final SqlSettings sql;
    private final SqlSettings localSql;
    private final MongoSettings mongo;
    private final RedisSettings redis;
    private final boolean fallbackEnabled;

    public StorageSettings(
            String serverId,
            ScoreRouting scores,
            LocationRouting locations,
            SqlSettings sql,
            SqlSettings localSql,
            MongoSettings mongo,
            RedisSettings redis,
            boolean fallbackEnabled
    ) {
        this.serverId = (serverId == null || serverId.isBlank()) ? "lobby" : serverId;
        this.scores = scores == null ? new ScoreRouting(StorageBackendType.LOCAL_SQL) : scores;
        this.locations = locations == null ? new LocationRouting(StorageBackendType.LOCAL_SQL, StorageBackendType.NONE) : locations;
        this.sql = sql == null ? SqlSettings.empty() : sql;
        this.localSql = localSql == null ? SqlSettings.empty() : localSql;
        this.mongo = mongo == null ? MongoSettings.empty() : mongo;
        this.redis = redis == null ? RedisSettings.empty() : redis;
        this.fallbackEnabled = fallbackEnabled;
    }

    public String serverId() {
        return serverId;
    }

    public ScoreRouting scores() {
        return scores;
    }

    public LocationRouting locations() {
        return locations;
    }

    public SqlSettings sql() {
        return sql;
    }

    public SqlSettings localSql() {
        return localSql;
    }

    public MongoSettings mongo() {
        return mongo;
    }

    public RedisSettings redis() {
        return redis;
    }

    public boolean fallbackEnabled() {
        return fallbackEnabled;
    }

    public record ScoreRouting(StorageBackendType primary) {
    }

    public record LocationRouting(StorageBackendType primary, StorageBackendType cache) {
    }

    public record SqlSettings(String url, String user, String password, String driver) {
        public static SqlSettings empty() {
            return new SqlSettings("", "", "", "");
        }
    }

    public record MongoSettings(String uri, String database, String collectionPrefix) {
        public static MongoSettings empty() {
            return new MongoSettings("", "", "hub_");
        }
    }

    public record RedisSettings(String uri, int database, String keyPrefix) {
        public static RedisSettings empty() {
            return new RedisSettings("", 0, "hub");
        }
    }
}
