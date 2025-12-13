package net.uebliche.hub.common.update;

public record UpdateCheckResult(boolean updateAvailable, String latestVersion, boolean aheadOfRelease) {
    public static UpdateCheckResult unavailable() {
        return new UpdateCheckResult(false, null, false);
    }
}
