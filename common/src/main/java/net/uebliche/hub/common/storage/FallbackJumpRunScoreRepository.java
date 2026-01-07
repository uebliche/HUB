package net.uebliche.hub.common.storage;

import java.util.UUID;

public final class FallbackJumpRunScoreRepository implements JumpRunScoreRepository {
    private final JumpRunScoreRepository primary;
    private final JumpRunScoreRepository fallback;
    private final StorageLogger logger;

    public FallbackJumpRunScoreRepository(JumpRunScoreRepository primary, JumpRunScoreRepository fallback, StorageLogger logger) {
        this.primary = primary;
        this.fallback = fallback;
        this.logger = logger == null ? StorageLogger.noop() : logger;
    }

    @Override
    public JumpRunScore getScore(String courseId, UUID playerId) {
        try {
            return primary.getScore(courseId, playerId);
        } catch (Exception ex) {
            logger.warn("Primary score storage failed, using fallback: " + ex.getMessage());
            return fallback.getScore(courseId, playerId);
        }
    }

    @Override
    public JumpRunScore recordRun(String courseId, UUID playerId, long timeMillis) {
        try {
            return primary.recordRun(courseId, playerId, timeMillis);
        } catch (Exception ex) {
            logger.warn("Primary score storage failed, using fallback: " + ex.getMessage());
            return fallback.recordRun(courseId, playerId, timeMillis);
        }
    }

    @Override
    public void close() {
        try {
            primary.close();
        } catch (Exception ex) {
            logger.warn("Failed to close primary score storage: " + ex.getMessage());
        }
        try {
            fallback.close();
        } catch (Exception ex) {
            logger.warn("Failed to close fallback score storage: " + ex.getMessage());
        }
    }
}
