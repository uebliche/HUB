package net.uebliche.hub.common.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RedisPlayerLocationRepository implements PlayerLocationRepository {
    private final RedisClientProvider provider;

    public RedisPlayerLocationRepository(StorageSettings.RedisSettings settings) {
        this.provider = new RedisClientProvider(settings);
    }

    @Override
    public PlayerLocation getLocation(String serverId, UUID playerId) {
        String key = locationKey(serverId, playerId);
        return provider.withJedis(jedis -> {
            Map<String, String> data = jedis.hgetAll(key);
            if (data == null || data.isEmpty()) {
                return null;
            }
            return new PlayerLocation(
                    serverId,
                    playerId,
                    data.getOrDefault("world", ""),
                    parseDouble(data.get("x")),
                    parseDouble(data.get("y")),
                    parseDouble(data.get("z")),
                    parseFloat(data.get("yaw")),
                    parseFloat(data.get("pitch")),
                    parseLong(data.get("updated_at"))
            );
        });
    }

    @Override
    public void saveLocation(PlayerLocation location) {
        String key = locationKey(location.serverId(), location.playerId());
        provider.withJedis(jedis -> {
            Map<String, String> update = new HashMap<>();
            update.put("world", location.world() == null ? "" : location.world());
            update.put("x", Double.toString(location.x()));
            update.put("y", Double.toString(location.y()));
            update.put("z", Double.toString(location.z()));
            update.put("yaw", Float.toString(location.yaw()));
            update.put("pitch", Float.toString(location.pitch()));
            update.put("updated_at", Long.toString(location.updatedAt()));
            jedis.hset(key, update);
            return null;
        });
    }

    @Override
    public void close() {
        provider.close();
    }

    private String locationKey(String serverId, UUID playerId) {
        return provider.keyPrefix() + ":loc:" + serverId + ":" + playerId;
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return 0.0D;
        }
    }

    private static float parseFloat(String value) {
        if (value == null || value.isBlank()) {
            return 0.0F;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ex) {
            return 0.0F;
        }
    }
}
