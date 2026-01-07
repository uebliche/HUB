package net.uebliche.hub.common.storage;

import java.util.UUID;

public final class NoopJumpRunScoreRepository implements JumpRunScoreRepository {
    @Override
    public JumpRunScore getScore(String courseId, UUID playerId) {
        return null;
    }

    @Override
    public JumpRunScore recordRun(String courseId, UUID playerId, long timeMillis) {
        return null;
    }

    @Override
    public void close() {
    }
}
