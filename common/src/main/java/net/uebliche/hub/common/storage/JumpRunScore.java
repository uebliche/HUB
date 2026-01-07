package net.uebliche.hub.common.storage;

import java.util.UUID;

public record JumpRunScore(String courseId, UUID playerId, long runCount, long bestTimeMillis, long updatedAt) {
}
