package net.uebliche.hub.common.storage;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;
import java.util.function.Function;

final class RedisClientProvider {
    private final JedisPool pool;
    private final int database;
    private final String keyPrefix;

    RedisClientProvider(StorageSettings.RedisSettings settings) {
        String uri = settings.uri();
        if (uri == null || uri.isBlank()) {
            throw new StorageException("Redis uri is not configured");
        }
        this.pool = new JedisPool(new JedisPoolConfig(), URI.create(uri));
        this.database = Math.max(0, settings.database());
        String prefix = settings.keyPrefix();
        this.keyPrefix = (prefix == null || prefix.isBlank()) ? "hub" : prefix;
    }

    <T> T withJedis(Function<Jedis, T> fn) {
        try (Jedis jedis = pool.getResource()) {
            if (database > 0) {
                jedis.select(database);
            }
            return fn.apply(jedis);
        }
    }

    String keyPrefix() {
        return keyPrefix;
    }

    void close() {
        pool.close();
    }
}
