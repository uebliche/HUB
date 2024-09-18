package io.freddi.hub;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;

public class UpdateChecker {

    private final Logger logger;
    public Boolean updateAvailable = false;
    public Hub hub;
    public String latest = Props.VERSION;
    private boolean running;

    public UpdateChecker(Hub hub, Logger logger) {
        this.hub = hub;
        this.logger = logger;
        Executors.newVirtualThreadPerTaskExecutor().execute(this::checkForUpdates);
    }

    private void checkForUpdates() {
        if (running)
            return;
        while (hub.config.updateChecker.enabled && !updateAvailable) {
            running = true;
            HttpClient client = HttpClient.newHttpClient();
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
                JsonObject lastestVersion = body.getAsJsonArray().get(0).getAsJsonObject();
                ModuleDescriptor.Version latest = ModuleDescriptor.Version.parse(lastestVersion.get("version_number").getAsString());
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
                Thread.sleep((long) Math.max(hub.config.updateChecker.checkIntervalInMin, 5) * 60 * 1000);
            } catch (InterruptedException ignored) {
            }
        }
        running = false;

    }
}
