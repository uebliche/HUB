package net.uebliche.hub.common.model;

import java.util.List;

public record NavigatorEntrySpec(
        String name,
        List<String> lore,
        String icon,
        NavigatorAction action,
        String server,
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        int slot
) {
    public enum NavigatorAction {
        TELEPORT, SERVER, LOBBY_SELECTOR
    }
}
