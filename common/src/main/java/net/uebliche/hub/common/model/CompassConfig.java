package net.uebliche.hub.common.model;

import java.util.List;
import java.util.Map;

public record CompassConfig(
        boolean enabled,
        String guiTitle,
        ItemSpec item,
        String listItemNameTemplate,
        List<String> listItemLoreTemplate
) {
    public static CompassConfig fallback() {
        return new CompassConfig(
                true,
                "<aqua>Select Lobby",
                new ItemSpec("<gold>Lobby Compass", List.of("<gray>Right-click to choose a lobby"),
                        "MAGMA_CREAM", 0, false, false, true, true),
                "<gold><lobby>",
                List.of(
                        "<gray>Server: <yellow><server>",
                        "<gray>Players: <green><online></dark_gray>/<green><max>",
                        "<dark_gray>Click to join"
                )
        );
    }
}
