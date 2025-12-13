package net.uebliche.hub.common.model;

import java.util.List;

public record NavigatorConfig(
        boolean enabled,
        String guiTitle,
        int guiRows,
        ItemSpec item,
        List<NavigatorEntrySpec> entries,
        boolean openLobbySelectorOnRightClick
) {
    public static NavigatorConfig fallback() {
        return new NavigatorConfig(
                true,
                "<aqua>Navigator",
                0,
                new ItemSpec("<gold>Navigator", List.of("<gray>Open navigator"), "COMPASS", 4, false, false, true, true),
                List.of(
                        new NavigatorEntrySpec("<gold>Spawn", List.of("<gray>Teleport to spawn"), "COMPASS",
                                NavigatorEntrySpec.NavigatorAction.TELEPORT, "", "world", 0, 64, 0, 0f, 0f, 13),
                        new NavigatorEntrySpec("<gold>Lobbies", List.of("<gray>Open lobby selector"), "BOOK",
                                NavigatorEntrySpec.NavigatorAction.LOBBY_SELECTOR, "", "world", 0, 64, 0, 0f, 0f, 11),
                        new NavigatorEntrySpec("<gold>Lobby", List.of("<gray>Join lobby server"), "ENDER_PEARL",
                                NavigatorEntrySpec.NavigatorAction.SERVER, "lobby", "world", 0, 64, 0, 0f, 0f, 15),
                        new NavigatorEntrySpec("<gold>Premium Lobby", List.of("<gray>Join premium lobby"), "DIAMOND",
                                NavigatorEntrySpec.NavigatorAction.SERVER, "premiumlobby", "world", 0, 64, 0, 0f, 0f, 21),
                        new NavigatorEntrySpec("<gold>Team Lobby", List.of("<gray>Join team lobby"), "GOLD_INGOT",
                                NavigatorEntrySpec.NavigatorAction.SERVER, "teamlobby", "world", 0, 64, 0, 0f, 0f, 23),
                        new NavigatorEntrySpec("<gold>World Spawn", List.of("<gray>Go to world spawn"), "AMETHYST_SHARD",
                                NavigatorEntrySpec.NavigatorAction.TELEPORT, "", "world", 0, 80, 0, 0f, 0f, 31)
                ),
                false
        );
    }
}
