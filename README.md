<!-- modrinth_exclude.start -->

# Hub Plugin for Velocity Proxy

<!-- modrinth_exclude.end -->

This project is a **Hub Plugin** for the **Velocity Proxy Server**. It automatically connects players to the best
available lobby servers based on permissions, server load, and other criteria. The plugin includes advanced debugging
features and offers full customization through a configuration file.

## Features

- **Automatic Server Selection**: Automatically connects players to the best available lobby server.
- **Multiple Lobbies**: Supports multiple lobby types with individual permissions and priorities.
- **Customizable Messages**: Success messages and system notifications can be fully customized using **MiniMessage**
  syntax.
- **Dynamic Commands**: Fully customizable commands and permissions for different lobbies.
- **Debugging Mode**: Enable debug mode to troubleshoot issues with detailed messages.
- **Placeholder Support**: Supports placeholders for server and player information.

## Installation

1. **Requirements**:

    - Velocity Proxy Server (latest version)
    - **Java 21**

2. **Plugin Download**:

    - [Download here from Modrinth](https://modrinth.com/project/hub)

3. **Installation**:

    - Place the JAR file in the `plugins` directory of your Velocity server.

4. **Restart the Server**:

    - Restart your Velocity server to load the plugin.

## Configuration

After the server's first start, a `config.yml` file will be generated in the plugin directory. This file allows full
customization, including commands, permissions, messages, and server behaviors. You can adjust everything to meet your
server's needs.

### MiniMessage and Regex Support

The plugin supports **MiniMessage** syntax for customizing messages. You can easily style your messages using
MiniMessage tags like `<i>`, `<b>`, and `<color>`. For a quick preview or to test your MiniMessage strings, visit
the [MiniMessage web editor](https://webui.advntr.dev/).

For custom server filters and other patterns, **Regex** is supported. Test your expressions easily with
a [Regex editor](https://regex101.com/).

### Example Configuration

```yaml
messages:
  success-message: <#69d9ff>You are now in the <i>Hub</i>.
  already-connected-message: <#ff614d>You are already on the <i>Hub</i>.
  connection-in-progress-message: <#ff9c59>In Progress...
  server-disconnected-message: <#ff614d>The Lobby Server is Offline...
  connection-cancelled-message: <#ff614d>Transfer cancelled.
system-messages:
  players-only-command-message: <#ff9c59>This Command is only available to Players.
  no-lobby-found-message: <#ff9c59>I'm sorry! I was unable to find a Lobby Server for you.
aliases:
  - lobby
  - leave
base-hub-command: hub
debug:
  enabled: true
  permission: hub.debug
lobbies:
  - name: teamlobby
    filter: (?i)^teamlobby.*
    permission: hub.team
    priority: 2
    commands:
      teamlobby:
        standalone: false
        subcommand: true
    autojoin: false
  - name: premiumlobby
    filter: (?i)^premiumlobby.*
    permission: ''
    priority: 1
    commands:
      premiumlobby:
        standalone: true
        subcommand: false
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
    autojoin: true
placeholder:
  server:
    enabled: true
  lobby:
    enabled: true
  player:
    enabled: true
auto-select:
  on-join: true
  on-server-kick: true
ping-duration-in-millis: 20
finder:
  start-duration: 20
  increment-duration: 20
  max-duration: 200
```

You can customize all commands, permissions, and other settings directly in the configuration file to suit your server's
requirements.

<!-- modrinth_exclude.start -->
For further questions or support, visit the [Modrinth project page](https://modrinth.com/project/hub).
<!-- modrinth_exclude.end -->