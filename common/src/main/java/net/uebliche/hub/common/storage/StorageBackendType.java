package net.uebliche.hub.common.storage;

import java.util.Locale;

public enum StorageBackendType {
    SQL,
    MONGO,
    REDIS,
    LOCAL_SQL,
    NONE;

    public static StorageBackendType fromString(String raw) {
        if (raw == null) {
            return NONE;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "sql" -> SQL;
            case "mongo", "mongodb" -> MONGO;
            case "redis" -> REDIS;
            case "local-sql", "local_sql", "sqlite" -> LOCAL_SQL;
            case "none", "off", "disabled" -> NONE;
            default -> NONE;
        };
    }

    public boolean isSql() {
        return this == SQL || this == LOCAL_SQL;
    }
}
