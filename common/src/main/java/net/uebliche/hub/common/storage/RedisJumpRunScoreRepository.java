package net.uebliche.hub.common.storage;

import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RedisJumpRunScoreRepository implements JumpRunScoreRepository {
    private final RedisClientProvider provider;

    public RedisJumpRunScoreRepository(StorageSettings.RedisSettings settings) {
        this.provider = new RedisClientProvider(settings);
    }

    @Override
    public JumpRunScore getScore(String courseId, UUID playerId) {
        String key = scoreKey(courseId, playerId);
        return provider.withJedis(jedis -> {
            Map<String, String> data = jedis.hgetAll(key);
            if (data == null || data.isEmpty()) {
                return null;
            }
            long runCount = parseLong(data.get("run_count"));
            long bestTime = parseLong(data.get("best_time_ms"));
            long updatedAt = parseLong(data.get("updated_at"));
            return new JumpRunScore(courseId, playerId, runCount, bestTime, updatedAt);
        });
    }

    @Override
    public JumpRunScore recordRun(String courseId, UUID playerId, long timeMillis) {
        String key = scoreKey(courseId, playerId);
        return provider.withJedis(jedis -> {
            Map<String, String> data = jedis.hgetAll(key);
            long runCount = parseLong(data.get("run_count"));
            long bestTime = parseLong(data.get("best_time_ms"));
            long now = System.currentTimeMillis();
            runCount += 1;
            if (timeMillis > 0 && (bestTime <= 0 || timeMillis < bestTime)) {
                bestTime = timeMillis;
            }
            Map<String, String> update = new HashMap<>();
            update.put("run_count", Long.toString(runCount));
            update.put("best_time_ms", Long.toString(bestTime));
            update.put("updated_at", Long.toString(now));
            jedis.hset(key, update);
            return new JumpRunScore(courseId, playerId, runCount, bestTime, now);
        });
    }

    @Override
    public void close() {
        provider.close();
    }

    private String scoreKey(String courseId, UUID playerId) {
        return provider.keyPrefix() + ":score:" + courseId + ":" + playerId;
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
}
