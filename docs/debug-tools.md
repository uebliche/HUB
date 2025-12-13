# Debug Toolkit

HUB ships with a dedicated debug command that reveals how lobby selection works and helps validate your configuration
without restarting the proxy.

## Access

- Console always has access.
- Players require the permission defined in `debug.permission` (default `hub.debug`).
- Debug output is only broadcast when `debug.enabled` is `true`.

## `/hub debug` Commands

| Subcommand | Purpose |
| --- | --- |
| `enable` / `disable` | Toggle debug mode and persist the value back into `config.yml`. |
| `reload` | Reload `config.yml`, resort lobbies, and re-register commands. Errors are printed to the invoker. |
| `messages` | Preview every global message and lobby override using live placeholder data. Useful for MiniMessage tweaks. |
| `placeholders <lobby> <server>` | Dump every enabled placeholder for the given lobby/server pair. Picks values from the ping cache, so run `/hub debug reload` if the cache is stale. |

Each previewed message is sent back to the invoker prefixed by `[Debug]:` and can be safely copied into the config file.

## Ping Cache Insights

HUB keeps a cache of the most recent ping result for every server that matches a lobby filter. With debug mode enabled
it logs the following events:

- When the cache refresh starts and finishes.
- Whether a ping succeeded, failed, or returned no data.
- Which lobby/server combination was selected for a player, including latency, player counts, and cache age.

You can force a refresh before testing by running `/hub debug placeholders <lobby> <server>` or simply using `/hub` to
trigger the lobby finder.

## Update Checker Logging

When the update checker is enabled it logs the following to the console and to debugging players:

- Start of each Modrinth poll (`🔄 Checking for Updates...`).
- Success messages if a newer version is available.
- Warnings when the proxy is running a newer version than any release or when the HTTP check fails.

Players that pass the `notification` permission receive the configured MiniMessage on login after a newer version is
found. The message uses `<current>` and `<latest>` placeholders filled in by the plugin.

## Troubleshooting Tips

- If no lobby is selected, verify the `filter` regex against the registered server names (debug output will list empty
  caches and missing servers).
- Ensure players actually hold the permission required by the target lobby. Debug logging prints the decision for each
  lobby in order.
- Use the placeholder dump to confirm that MiniMessage keys match the placeholder definitions in `config.yml`.
