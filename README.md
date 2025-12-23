<!-- modrinth_exclude.start -->

# HUB: Multi-loader Lobby Router

<!-- modrinth_exclude.end -->

This project is a **Hub Plugin** for your network. It runs on Velocity (primary) and ships sibling loaders for Paper, Fabric, NeoForge, and Minestom so you can keep players on the best lobby server that matches their permissions and your routing rules. The plugin leans on MiniMessage for fully styled output and ships with a rich debugging toolkit so you can see exactly what it is doing.

## Features at a Glance
- Multi-loader: Velocity (primary), Paper, Fabric, NeoForge, Minestom.
- Priority and permission aware routing with parent lobby groups for hierarchical fallbacks.
- Smart ping cache with fast refresh on empty cache/redirects.
- Configurable commands and MiniMessage placeholders.
- Debug toolkit (`/hub debug`) and Modrinth update checks.

## Config Builder (Web)
Use the web-based config editor to build configs without touching YAML:
https://hub.uebliche.info/

What it gives you:
- Visual editor for lobbies, regex, commands, and groups.
- Drag and drop grouping for parent groups.
- Download a ready-to-use `config.yml`.

## Tested Versions

<!-- tested_versions:start -->
- `latest`
<!-- tested_versions:end -->

- **Priority based routing**: Sorts lobbies by priority and checks player permissions before choosing the target server.
- **Smart ping cache**: Continuously pings lobby servers on a background schedule and reuses the freshest result to keep
  joins instant.
- **Configurable commands**: Change the base command name, add aliases, and expose per-lobby commands as standalone
  entries or as `/hub` subcommands.
- **Regex driven matching**: Use Java regular expressions to match Lobby filters and to hide commands on specific
  servers.
- **MiniMessage placeholders**: Toggle ready-made placeholders for server, lobby, and player data without touching the
  code.
- **Debug toolkit**: `/hub debug` lets you enable logging, reload the config, preview MiniMessage output, and inspect
  the ping cache in game.
- **Automatic update checks**: Polls Modrinth on a configurable interval and notifies privileged players when an update
  is available.
- **Safe fallbacks**: Can automatically move players to a lobby when they join or when they are kicked from another
  server.

## Getting Started

1. **Requirements**
   - Velocity Proxy (latest release)
   - **Java 21** runtime
2. **Download**
   - [Latest release on Modrinth](https://modrinth.com/plugin/hub)
3. **Install**
   - Drop the JAR into the `plugins` directory of your Velocity server and restart once.
4. **Configure**
   - Inspect the generated `plugins/hub/config.yml` (Velocity) or platform equivalent and adjust it to your network. Reload with `/hub debug reload` or restart the proxy after changes.
5. **Optional: Use the Config Builder**
   - Open https://hub.uebliche.info/, upload or paste your config, edit, then download the updated YAML.

## Commands

- `/{base-hub-command}`: Sends the player to the best matching lobby. The default is `/hub` and any aliases defined in
  `aliases` are registered as well.
- Standalone lobby commands: Configure under `lobbies.<name>.commands.<alias>.standalone`. When set to `true`, the alias
  is registered as its own top level Velocity command.
- Sub commands: When `commands.<alias>.subcommand` is `true`, the alias becomes available as `/hub <alias>`.
- **Hiding commands**: `hide-hub-command-on-lobby` prevents a player from executing the base command when their current
  server name matches the provided regex.

## Debug Toolkit

Use `/hub debug` (requires the permission in `debug.permission` or console access) to access runtime tools:

- `/hub debug enable` and `/hub debug disable`: Toggle debug mode and persist the flag back to the configuration file.
- `/hub debug reload`: Reload the configuration from disk and re-register commands without restarting.
- `/hub debug messages`: Preview every global and lobby specific MiniMessage string using live placeholder data.
- `/hub debug placeholders <lobby> <server>`: Render every enabled placeholder for the selected lobby/server pair so you
  can copy the exact placeholder names into your messages.

Debug output is prefixed with `[Debug]:` and is only sent to console or to players that hold the configured debug
permission.

## Automatic Routing

`auto-select.on-join` sends players to their best lobby as soon as they log in. `auto-select.on-server-kick` catches
kick events from other servers and immediately routes the player to the next best lobby. If no lobby can be found, the
player receives the `system-messages.no-lobby-found-message` and is disconnected.

## Configuration Overview

All options live inside `plugins/hub/config.yml` (Velocity) and are automatically written back with helpful defaults.

- `debug`: Controls whether debug output is active and which permission is required to toggle it in game.
- `messages` & `system-messages`: Global MiniMessage templates used when a lobby does not override them.
- `base-hub-command`, `aliases`, `hide-hub-command-on-lobby`: Configure command names and optionally hide the base command when the player is already on a matching server.
- `auto-select`: Enables automatic routing on join and/or after a kick.
- `lobby-groups`: Named lists of lobbies you can reference from `parent-groups` to build hierarchical fallbacks.
- `lobbies`: Describes every lobby type with name, regex filter, permission, priority, command exposure, optional message overrides, and best-effort `autojoin` flag.
- `placeholder`: Enables or disables groups of placeholders. Each entry exposes the `key` injected into MiniMessage and an example value.
- `finder`: Tunes the ping cache. `refresh-interval-in-ticks` defines how often every matching server is pinged, while `start-duration` and `max-duration` are ping timeouts.
- `update-checker`: Enables the Modrinth update task. `notification` is used both as the permission and MiniMessage string when an update is available. `check-interval-in-min` defines the polling cadence.

## How-to Recipes
- Add a new lobby: create a new lobby entry, set `filter`, `permission`, and `priority`, then (optional) add a command.
- Create lobby groups: define a group with `name` and `lobbies`, then use `parent-group` to nest groups.
- Set parent fallbacks: add `parent` for a single parent lobby or `parent-groups` for group-based fallbacks.
- Hide /hub on certain servers: set `hide-hub-command-on-lobby` to a regex that matches server names.
- Enable update notifications: set `update-checker.enabled` to true and add a permission string in `notification`.

### Placeholder Reference

| Category | Key | Default example | Enabled | Description |
| --- | --- | --- | --- | --- |
| Server | `server` | `lobby-1` | Yes | Registered server name that the player is sent to. |
| Server | `server-host` | `127.0.0.1` | No | Host of the registered server. |
| Server | `server-port` | `25565` | No | Port of the registered server. |
| Server | `server-player-count` | `0` | Yes | Amount of players currently connected to that server. |
| Server | `server-player-%i-username` | `apitoken` | No | Username of indexed players on the target server (virtual placeholders per index). |
| Server | `server-player-%i-uuid` | `f9de374c-cb78-4c5c-aa2f-4a53ae981f9d` | No | UUID of indexed players on the target server. |
| Lobby | `lobby` | `lobby` | Yes | Lobby name from the config. |
| Lobby | `lobby-filter` | `(?i)^lobby.*` | No | Regex filter configured for the lobby. |
| Lobby | `lobby-require-permission` | `true` | No | Indicates whether a permission is required. |
| Lobby | `lobby-permission` | `hub.user` | Yes | Permission needed to access the lobby. |
| Lobby | `lobby-priority` | `0` | No | Lobby priority used for routing. |
| Lobby | `lobby-command-%s-standalone` | `true` | No | Whether the named command is registered as standalone. |
| Lobby | `lobby-command-%s-subcommand` | `true` | No | Whether the named command is available as a `/hub` subcommand. |
| Lobby | `lobby-command-%s-hide-on` | `^(?!.*).$` | No | Regex used to hide a lobby specific command (reserved for future use). |
| Lobby | `lobby-autojoin` | `true` | No | Indicates whether the lobby prefers automatic joins (exposed for messaging only). |
| Player | `player` | `Freddiio` | Yes | Player username. |
| Player | `player-uuid` | `c5eb5df7-b7a9-4919-a9bc-7f59c8bee980` | No | Player UUID. |

## Update Checker Notes

The update checker polls Modrinth using the configured interval (minimum five minutes). When a newer version is
available, players that satisfy the `notification` permission receive the MiniMessage defined in that field with
`<current>` and `<latest>` placeholders filled in. Until the implementation switches to `notification-message`, be sure
that the `notification` value is a valid MiniMessage string (and optionally includes `[permission]`-style checks via
Velocity permissions).

<!-- modrinth_exclude.start -->
For further questions or support, visit the [Modrinth project page](https://modrinth.com/plugin/hub).
<!-- modrinth_exclude.end -->
