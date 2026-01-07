package net.uebliche.hub.common.storage;

import java.util.UUID;

public interface JumpRunScoreRepository extends AutoCloseable {
    JumpRunScore getScore(String courseId, UUID playerId) throws StorageException;

    JumpRunScore recordRun(String courseId, UUID playerId, long timeMillis) throws StorageException;

    @Override
    void close();
}
