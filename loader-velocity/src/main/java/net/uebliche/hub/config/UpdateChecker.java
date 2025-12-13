package net.uebliche.hub.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class UpdateChecker {
    public boolean enabled = true;
    public String notification = "";
    public Integer checkIntervalInMin = 30;
}
