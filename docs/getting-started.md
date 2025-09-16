# Getting Started

Follow these steps to install the HUB plugin on a Velocity proxy and verify that lobby routing works as expected.

## Requirements

- Velocity Proxy (latest release)
- Java 21 runtime
- At least one registered lobby server in `velocity.toml`

## Installation

1. Download the latest JAR from [Modrinth](https://modrinth.com/plugin/hub) or from your build pipeline.
2. Place the file inside the `plugins` folder of your Velocity proxy.
3. Start or restart the proxy. HUB will create `plugins/hub/config.yml` on the first run.
4. Review the generated configuration and adjust the lobby definitions to match the server names on your network.

## Verifying the Setup

- Use `/hub` (or the base command you configured) while connected to any backend server. You should be routed to the
  highest priority lobby you have permission to join.
- Toggle debug mode with `/hub debug enable` and repeat the command. You will receive detailed messages describing the
  selection process, including cached ping data and permission checks.
- Try `/hub debug messages` to preview every MiniMessage string with live placeholder values.

## Adding Lobbies

Each lobby entry contains:

- `name`: Friendly identifier shown only in debug output and placeholders.
- `filter`: Java regular expression that must match the registered Velocity server name.
- `permission`: Permission a player must have. Leave empty to allow everyone.
- `priority`: Higher priorities are tried first.
- `commands`: Map of command names with `standalone` (register as its own command) and `subcommand` (register under
  `/hub`).
- `autojoin`: Informational flag exposed in placeholders (the routing logic uses permissions and priorities).
- `overwrite-messages`: Optional per-lobby MiniMessage overrides.

After editing `config.yml` you can apply the changes at runtime with `/hub debug reload`.

## Automatic Transfers

Set `auto-select.on-join` to `true` to send players directly to a lobby after login, and `auto-select.on-server-kick`
so that kicked players land in a safe hub instead of being disconnected.

## Next Steps

- Review the [Configuration Guide](/configuration) for a deep dive into every option.
- Explore the [Debug Toolkit](/debug-tools) to monitor routing and inspect placeholder output while testing.
