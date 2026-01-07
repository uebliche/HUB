package net.uebliche.hub.common.storage;

public interface StorageLogger {
    void info(String message);

    void warn(String message);

    void error(String message, Throwable error);

    static StorageLogger noop() {
        return new StorageLogger() {
            @Override
            public void info(String message) {
            }

            @Override
            public void warn(String message) {
            }

            @Override
            public void error(String message, Throwable error) {
            }
        };
    }
}
