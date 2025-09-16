package net.uebliche.hub.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class UpdateChecker {

    public boolean enabled = true;
    public String notification = "hub.update";
    public Integer checkIntervalInMin = 60 * 6;
    public String notificationMessage = """
            An update is available! Latest version: <latest>, you are using: <current>
            Download it at https://modrinth.com/plugin/hub/version/<latest>
            """;


    public UpdateChecker() {
    }

    public UpdateChecker(boolean enabled, String notification, Integer checkIntervalInMin) {
        this.enabled = enabled;
        this.notification = notification;
        this.checkIntervalInMin = checkIntervalInMin;

    }

    public boolean enabled() {
        return enabled;
    }

    public UpdateChecker setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String notification() {
        return notification;
    }

    public UpdateChecker setNotification(String notification) {
        this.notification = notification;
        return this;
    }

    public Integer checkIntervalInMin() {
        return checkIntervalInMin;
    }

    public UpdateChecker setCheckIntervalInMin(Integer checkIntervalInMin) {
        this.checkIntervalInMin = checkIntervalInMin;
        return this;
    }
}