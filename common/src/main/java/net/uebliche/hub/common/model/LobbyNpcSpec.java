package net.uebliche.hub.common.model;

import java.util.List;

public record LobbyNpcSpec(
        String id,
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String name,
        List<String> lines,
        String entity,
        NavigatorEntrySpec.NavigatorAction action,
        String server,
        String targetWorld,
        double targetX,
        double targetY,
        double targetZ,
        float targetYaw,
        float targetPitch
) {
}
