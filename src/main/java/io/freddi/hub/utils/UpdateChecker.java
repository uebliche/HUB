package io.freddi.hub.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.freddi.hub.Hub;
import io.freddi.hub.Props;

import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;

public class UpdateChecker extends Utils<UpdateChecker> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("hub | updatechecker");
    public Boolean updateAvailable = false;
    public String latest = Props.VERSION;
    private boolean running;

    public UpdateChecker(Hub hub) {
        super(hub);
        Executors.newVirtualThreadPerTaskExecutor().execute(this::checkForUpdates);
    }

    private void checkForUpdates() {
        if (running)
            return;
        ConfigUtils configUtils = Utils.util(ConfigUtils.class);
        MessageUtils messageUtils = Utils.util(MessageUtils.class);
        HttpClient client = HttpClient.newHttpClient();
        while (configUtils.config().updateChecker.enabled && !updateAvailable) {
            messageUtils.broadcastDebugMessage("🔎 Checking for Updates...");
            running = true;
            try {
                var body = JsonParser.parseString(
                        client.send(
                                HttpRequest.newBuilder()
                                        .uri(URI.create("https://api.modrinth.com/v2/project/HrTclB8n/version"))
                                        .setHeader("User-Agent", "HUB " + Props.VERSION)
                                        .timeout(Duration.ofSeconds(30))
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.ofString()
                        ).body()
                );
                if (body == null) {
                    logger.warn("Unable to check for Updates!");
                    return;
                }
                JsonObject latestVersion = body.getAsJsonArray().get(0).getAsJsonObject();
                ModuleDescriptor.Version latest = ModuleDescriptor.Version.parse(latestVersion.get("version_number").getAsString());
                ModuleDescriptor.Version current = ModuleDescriptor.Version.parse(Props.VERSION);
                int compare = latest.compareTo(current);
                if (compare > 0) {
                    this.latest = latest.toString();
                    logger.info("An update is available! Latest version: {}, you are using: {}", latest, current);
                    logger.info("Download it at https://modrinth.com/plugin/hub/version/{}", latest);
                    updateAvailable = true;
                } else if (compare < 0) {
                    logger.warn("You are running " + Props.VERSION + " (a newer version of the plugin than released).");
                }

            } catch (Exception ignored) {
                logger.warn("Failed to check for updates");
            }
            try {
                Thread.sleep((long) Math.max(configUtils.config().updateChecker.checkIntervalInMin, 5) * 60 * 1000);
            } catch (InterruptedException ignored) {
            }
        }
        client.close();
        running = false;

    }
}
