package net.uebliche.hub.utils;

import net.uebliche.hub.Hub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Utils<T extends Utils<?>> implements AutoCloseable {

    private static final Map<Class<?>, Utils<?>> UTILS = new ConcurrentHashMap<>();
    protected final org.slf4j.Logger logger;
    protected final Hub hub;

    public Utils(Hub hub) {
        this.logger = org.slf4j.LoggerFactory.getLogger("hub | " + this.getClass().getSimpleName().toLowerCase());
        this.hub = hub;
        var previous = UTILS.put(getClass(), this);
        if (previous != null && previous != this) {
            previous.closeQuietly();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T util(Class<T> utilClass) {
        return (T) UTILS.get(utilClass);
    }

    public static void shutdownAll() {
        UTILS.values().forEach(Utils::closeQuietly);
        UTILS.clear();
    }

    protected void closeQuietly() {
        try {
            close();
        } catch (Exception e) {
            logger.warn("Failed to close {}", getClass().getSimpleName(), e);
        }
    }

    @Override
    public void close() throws Exception {
        // default no-op
    }
}
