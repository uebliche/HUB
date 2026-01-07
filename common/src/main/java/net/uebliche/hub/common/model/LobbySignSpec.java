package net.uebliche.hub.common.model;

import java.util.List;

public record LobbySignSpec(
        String id,
        String world,
        int x,
        int y,
        int z,
        List<String> lines,
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
