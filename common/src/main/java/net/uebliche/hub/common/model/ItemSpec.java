package net.uebliche.hub.common.model;

import java.util.List;

public record ItemSpec(String name, List<String> lore, String material, int slot, boolean allowMove,
                       boolean allowDrop, boolean dropOnDeath, boolean restoreOnRespawn) {
}
