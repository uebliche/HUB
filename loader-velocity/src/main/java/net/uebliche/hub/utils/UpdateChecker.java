package net.uebliche.hub.utils;

import net.uebliche.hub.common.update.UpdateCheckResult;

public class UpdateChecker extends Utils<UpdateChecker> {
    public volatile boolean updateAvailable = false;
    public volatile String latest = "dev";

    public UpdateChecker(net.uebliche.hub.Hub hub) {
        super(hub);
    }

    public void update(UpdateCheckResult result) {
        if (result == null) {
            return;
        }
        this.updateAvailable = result.updateAvailable();
        this.latest = result.latestVersion() == null ? latest : result.latestVersion();
    }
}
