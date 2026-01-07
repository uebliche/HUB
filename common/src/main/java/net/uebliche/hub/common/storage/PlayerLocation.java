package net.uebliche.hub.common.storage;

import java.util.UUID;

public record PlayerLocation(String serverId, UUID playerId, String world, double x, double y, double z, float yaw, float pitch, long updatedAt) {
}
