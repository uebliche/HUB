package net.uebliche.hub.common.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

public final class UpdateChecker {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private UpdateChecker() {
    }

    public static UpdateCheckResult checkModrinth(String projectId, String currentVersion, Consumer<String> logger) {
        try {
            if (logger != null) {
                logger.accept("Checking for updates (Modrinth project " + projectId + ")...");
            }
            var responseBody = CLIENT.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://api.modrinth.com/v2/project/" + projectId + "/version"))
                            .header("User-Agent", "HUB " + currentVersion)
                            .timeout(Duration.ofSeconds(30))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();

            if (responseBody == null) {
                if (logger != null) {
                    logger.accept("Update check failed: empty response");
                }
                return UpdateCheckResult.unavailable();
            }

            var body = JsonParser.parseString(responseBody);
            if (!body.isJsonArray() || body.getAsJsonArray().isEmpty()) {
                if (logger != null) {
                    logger.accept("Update check failed: unexpected response");
                }
                return UpdateCheckResult.unavailable();
            }

            JsonObject latestVersion = body.getAsJsonArray().get(0).getAsJsonObject();
            String latestVersionString = latestVersion.get("version_number").getAsString();
            int compare = compareVersions(latestVersionString, currentVersion);
            if (compare > 0) {
                if (logger != null) {
                    logger.accept("Update available: " + latestVersionString + " (current " + currentVersion + ")");
                }
                return new UpdateCheckResult(true, latestVersionString, false);
            } else if (compare < 0) {
                if (logger != null) {
                    logger.accept("You are ahead of release (" + currentVersion + ")");
                }
                return new UpdateCheckResult(false, currentVersion, true);
            } else {
                if (logger != null) {
                    logger.accept("You are up to date (" + currentVersion + ")");
                }
                return new UpdateCheckResult(false, latestVersionString, false);
            }
        } catch (Exception ex) {
            if (logger != null) {
                logger.accept("Update check failed: " + ex.getMessage());
            }
            return UpdateCheckResult.unavailable();
        }
    }

    private static int compareVersions(String latest, String current) {
        try {
            ModuleDescriptor.Version latestVersion = ModuleDescriptor.Version.parse(latest);
            ModuleDescriptor.Version currentVersion = ModuleDescriptor.Version.parse(current);
            return latestVersion.compareTo(currentVersion);
        } catch (Exception e) {
            return latest.compareTo(current);
        }
    }
}
