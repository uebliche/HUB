<!-- modrinth_exclude.start -->

# Hub Plugin for Velocity Proxy

<!-- modrinth_exclude.end -->

This project is a **Hub Plugin** for the **Velocity Proxy Server**. It automatically keeps players on the best lobby
server that matches their permissions and your routing rules. The plugin leans on MiniMessage for fully styled output and
ships with a rich debugging toolkit so you can see exactly what it is doing.

## Features at a Glance

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
   - Inspect the generated `plugins/hub/config.yml` and adjust it to your network. Reload with `/hub debug reload` or
     restart the proxy after changes.

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

All options live inside `plugins/hub/config.yml` and are automatically written back with helpful defaults.

- `debug`: Controls whether debug output is active and which permission is required to toggle it in game.
- `messages` & `system-messages`: Global MiniMessage templates used when a lobby does not override them.
- `base-hub-command`, `aliases`, `hide-hub-command-on-lobby`: Configure the names under which the command is registered
  and optionally hide the base command when the player is already on a matching server.
- `auto-select`: Enables automatic routing on join and/or after a kick.
- `lobbies`: Describes every lobby type with a name, regex filter, permission, priority, command exposure, optional
  message overrides, and a best-effort `autojoin` flag (currently only exposed as a placeholder toggle).
- `placeholder`: Enables or disables groups of placeholders. Each entry exposes the `key` that is injected into
  MiniMessage and an example value.
- `finder`: Tunes the ping cache. `refresh-interval-in-ticks` defines how often every matching server is pinged, while
  `start-duration` and `max-duration` are used as ping timeouts.
- `update-checker`: Enables the Modrinth update task. `notification` is used both as the permission checked before
  showing the message and as the MiniMessage string that is sent when an update is available. `check-interval-in-min`
  defines the polling cadence. The `notification-message` field is currently not used by the code.

### Example Configuration

```yaml
# Thanks <3

debug:
  enabled: false
  permission: hub.debug
messages:
  success-message: <#69d9ff>You are now in the <i>Hub</i>.
  already-connected-message: <#ff614d>You are already on the <i>Hub</i>.
  connection-in-progress-message: <#ff9c59>In Progress...
  server-disconnected-message: <#ff614d>The Lobby Server is Offline...
  connection-cancelled-message: <#ff614d>Transfer cancelled.
system-messages:
  players-only-command-message: <#ff9c59>This Command is only available to Players.
  no-lobby-found-message: <#ff9c59>I'm sorry! i was unable to find a Lobby Server for you.
base-hub-command: hub
aliases:
  - lobby
  - leave
hide-hub-command-on-lobby: ^(?!.*).$
auto-select:
  on-join: true
  on-server-kick: true
lobbies:
  - name: teamlobby
    filter: (?i)^teamlobby.*
    permission: hub.team
    priority: 2
    commands:
      teamlobby:
        standalone: false
        subcommand: true
        hide-on: ^(?!.*).$
    autojoin: false
    overwrite-messages: {}
  - name: premiumlobby
    filter: (?i)^premiumlobby.*
    permission: hub.premium
    priority: 1
    commands:
      premiumlobby:
        standalone: true
        subcommand: false
        hide-on: ^(?!.*).$
    autojoin: true
    overwrite-messages:
      success-message: <#69d9ff>You are now in the <b>Premium Hub</b>.
  - name: lobby
    filter: (?i)^lobby.*
    permission: ''
    priority: 0
    commands:
      base:
        standalone: false
        subcommand: true
        hide-on: ^(?!.*).$
    autojoin: true
    overwrite-messages: {}
placeholder:
  server:
    example: lobby-1
    key: server
    enabled: true
  server-host:
    example: 127.0.0.1
    key: server-host
    enabled: false
  server-port:
    example: '25565'
    key: server-port
    enabled: false
  server-player-count:
    example: '0'
    key: server-player-count
    enabled: true
  server-player-per-player-username:
    example: apitoken
    key: server-player-%i-username
    enabled: false
    placeholder: '%i'
  server-player-per-player-uuid:
    example: f9de374c-cb78-4c5c-aa2f-4a53ae981f9d
    key: server-player-%1-uuid
    enabled: false
    placeholder: '%i'
  lobby:
    example: lobby
    key: lobby
    enabled: true
  lobby-filter:
    example: (?i)^lobby.*
    key: lobby-filter
    enabled: false
  lobby-require-permission:
    example: 'true'
    key: lobby-require-permission
    enabled: false
  lobby-permission:
    example: hub.user
    key: lobby-permission
    enabled: true
  lobby-priority:
    example: '0'
    key: lobby-priority
    enabled: false
  lobby-command-per-command-standalone:
    example: 'true'
    key: lobby-command-%s-standalone
    enabled: false
  lobby-command-per-command-subcommand:
    example: 'true'
    key: lobby-command-%s-subcommand
    enabled: false
  lobby-command-per-command-hide-on:
    example: ^(?!.*).$
    key: lobby-command-%s-hide-on
    enabled: false
  lobby-autojoin:
    example: 'true'
    key: lobby-autojoin
    enabled: false
  player:
    example: Freddiio
    key: player
    enabled: true
  player-uuid:
    example: c5eb5df7-b7a9-4919-a9bc-7f59c8bee980
    key: player-uuid
    enabled: false
finder:
  start-duration: 20
  increment-duration: 20
  max-duration: 200
  refresh-interval-in-ticks: 40
update-checker:
  enabled: true
  notification: hub.update
  check-interval-in-min: 360
  notification-message: |
    An update is available! Latest version: <latest>, you are using: <current>
    Download it at https://modrinth.com/plugin/hub/version/<latest>
```

### Placeholder Reference

| Category | Key | Default example | Enabled | Description |
| --- | --- | --- | --- | --- |
| Server | `server` | `lobby-1` | Yes | Registered server name that the player is sent to. |
| Server | `server-host` | `127.0.0.1` | No | Host of the registered server. |
| Server | `server-port` | `25565` | No | Port of the registered server. |
| Server | `server-player-count` | `0` | Yes | Amount of players currently connected to that server. |
| Server | `server-player-%i-username` | `apitoken` | No | Username of indexed players on the target server (virtual placeholders per index). |
| Server | `server-player-%1-uuid` | `f9de374c-cb78-4c5c-aa2f-4a53ae981f9d` | No | UUID of indexed players on the target server. |
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
