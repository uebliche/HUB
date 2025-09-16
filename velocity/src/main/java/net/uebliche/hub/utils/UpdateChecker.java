package net.uebliche.hub.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.uebliche.hub.Hub;
import net.uebliche.hub.Props;

import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateChecker extends Utils<UpdateChecker> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("hub | updatechecker");
    public volatile boolean updateAvailable = false;
    public volatile String latest = Props.VERSION;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final AtomicBoolean checking = new AtomicBoolean(false);
    private ScheduledTask task;

    public UpdateChecker(Hub hub) {
        super(hub);
        Utils.util(ConfigUtils.class).onReload(this::reschedule);
        reschedule();
    }

    public void reschedule() {
        cancelTask();
        var configUtils = Utils.util(ConfigUtils.class);
        if (configUtils == null || configUtils.config() == null) {
            return;
        }
        var updateConfig = configUtils.config().updateChecker;
        if (!updateConfig.enabled) {
            updateAvailable = false;
            latest = Props.VERSION;
            return;
        }
        long intervalMinutes = Math.max(updateConfig.checkIntervalInMin == null ? 0 : updateConfig.checkIntervalInMin, 5);
        task = hub.server().getScheduler()
                .buildTask(hub, this::runCheck)
                .delay(Duration.ZERO)
                .repeat(Duration.ofMinutes(intervalMinutes))
                .schedule();
    }

    private void runCheck() {
        var configUtils = Utils.util(ConfigUtils.class);
        if (configUtils == null || configUtils.config() == null) {
            return;
        }
        var updateConfig = configUtils.config().updateChecker;
        if (!updateConfig.enabled) {
            updateAvailable = false;
            latest = Props.VERSION;
            cancelTask();
            return;
        }
        if (!checking.compareAndSet(false, true)) {
            return;
        }

        try {
            var messageUtils = Utils.util(MessageUtils.class);
            if (messageUtils != null) {
                messageUtils.broadcastDebugMessage("🔄 Checking for Updates...");
            }

            var responseBody = client.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://api.modrinth.com/v2/project/HrTclB8n/version"))
                            .header("User-Agent", "HUB " + Props.VERSION)
                            .timeout(Duration.ofSeconds(30))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();

            if (responseBody == null) {
                logger.warn("Unable to check for Updates!");
                return;
            }

            var body = JsonParser.parseString(responseBody);
            if (!body.isJsonArray() || body.getAsJsonArray().isEmpty()) {
                logger.warn("Unexpected response from update API");
                return;
            }

            JsonObject latestVersion = body.getAsJsonArray().get(0).getAsJsonObject();
            String latestVersionString = latestVersion.get("version_number").getAsString();
            int compare = compareVersions(latestVersionString, Props.VERSION);
            if (compare > 0) {
                this.latest = latestVersionString;
                logger.info("An update is available! Latest version: {}, you are using: {}", latestVersionString, Props.VERSION);
                logger.info("Download it at https://modrinth.com/plugin/hub/version/{}", latestVersionString);
                updateAvailable = true;
            } else if (compare < 0) {
                logger.warn("You are running {} (a newer version of the plugin than released).", Props.VERSION);
            } else {
                updateAvailable = false;
                this.latest = Props.VERSION;
            }} catch (Exception e) {
            logger.warn("Failed to check for updates", e);
        } finally {
            checking.set(false);
        }
    }

    private int compareVersions(String latest, String current) {
        try {
            ModuleDescriptor.Version latestVersion = ModuleDescriptor.Version.parse(latest);
            ModuleDescriptor.Version currentVersion = ModuleDescriptor.Version.parse(current);
            return latestVersion.compareTo(currentVersion);
        } catch (IllegalArgumentException ignored) {
        }
        return latest.compareToIgnoreCase(current);
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void close() {
        cancelTask();
        checking.set(false);
    }
}

