package io.freddi.hub;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@ConfigSerializable
public class Config {

    public Debug debug = new Debug();

    public Messages messages = new Messages(
            "<#69d9ff>You are now in the <i>Hub</i>.",
            "<#ff614d>You are already on the <i>Hub</i>.",
            "<#ff9c59>In Progress...",
            "<#ff614d>The Lobby Server is Offline...",
            "<#ff614d>Transfer cancelled."
    );

    public SystemMessages systemMessages = new SystemMessages();


    public String baseHubCommand = "hub";
    public List<String> aliases = List.of("lobby", "leave");
    public Pattern hideHubCommandOnLobby = Pattern.compile("^(?!.*).$", Pattern.CASE_INSENSITIVE);

    public AutoSelect autoSelect = new AutoSelect();

    public List<Lobby> lobbies = List.of(
            new Lobby("lobby",
                    Pattern.compile("(?i)^lobby.*", Pattern.CASE_INSENSITIVE),
                    "",
                    0,
                    Map.of("base", new Command(false, true)),
                    true
            ),
            new Lobby("premiumlobby",
                    Pattern.compile("(?i)^premiumlobby.*", Pattern.CASE_INSENSITIVE),
                    "hub.premium",
                    1,
                    Map.of("premiumlobby", new Command(true, false)),
                    true
            ).setMessages(new Messages().setSuccessMessage(
                    "<#69d9ff>You are now in the <b>Premium Hub</b>."
            )),
            new Lobby("teamlobby",
                    Pattern.compile("(?i)^teamlobby.*", Pattern.CASE_INSENSITIVE),
                    "hub.team",
                    2,
                    Map.of("teamlobby", new Command(false, true)),
                    false
            )
    );
    public Placeholder placeholder = new Placeholder();
    public Finder finder = new Finder();
    public UpdateChecker updateChecker = new UpdateChecker();

    public Config() {
    }

    @ConfigSerializable
    public static class Finder {

        public int startDuration = 20;
        public int incrementDuration = 20;
        public int maxDuration = 200;

        public Finder() {
        }
    }

    @ConfigSerializable
    public static class AutoSelect {
        public boolean onJoin = true;
        public boolean onServerKick = true;

        public AutoSelect() {
        }

        public AutoSelect(boolean onJoin, boolean onServerKick) {
            this.onJoin = onJoin;
            this.onServerKick = onServerKick;
        }

        public boolean onJoin() {
            return onJoin;
        }

        public AutoSelect setOnJoin(boolean onJoin) {
            this.onJoin = onJoin;
            return this;
        }

        public boolean onServerKick() {
            return onServerKick;
        }

        public AutoSelect setOnServerKick(boolean onServerKick) {
            this.onServerKick = onServerKick;
            return this;
        }
    }

    @ConfigSerializable
    public static class Debug {
        public boolean enabled = false;
        public String permission = "hub.debug";
    }

    @ConfigSerializable
    public static class Placeholder {
        public Holder server = new Holder("server", "lobby-1");
        public Holder serverHost = new Holder("server-host", "127.0.0.1").setEnabled(false);
        public Holder serverPort = new Holder("server-port", "25565").setEnabled(false);

        public Holder serverPlayerCount = new Holder("server-player-count", "0");
        public Holder serverPlayerPerPlayerUsername = new Holder("server-player-%i-username", "apitoken").setEnabled(false).setPlaceholder("%i");
        public Holder serverPlayerPerPlayerUuid = new Holder("server-player-%1-uuid", "f9de374c-cb78-4c5c-aa2f-4a53ae981f9d").setEnabled(false).setPlaceholder("%i");

        public Holder lobby = new Holder("lobby", "lobby");
        public Holder lobbyFilter = new Holder("lobby-filter", "(?i)^lobby.*").setEnabled(false);
        public Holder lobbyRequirePermission = new Holder("lobby-require-permission", "true").setEnabled(false);
        public Holder lobbyPermission = new Holder("lobby-permission", "hub.user");
        public Holder lobbyPriority = new Holder("lobby-priority", "0").setEnabled(false);
        public Holder lobbyCommandPerCommandStandalone = new Holder("lobby-command-%s-standalone", "true").setEnabled(false);
        public Holder lobbyCommandPerCommandSubcommand = new Holder("lobby-command-%s-subcommand", "true").setEnabled(false);
        public Holder lobbyCommandPerCommandHideOn = new Holder("lobby-command-%s-hide-on", "^(?!.*).$").setEnabled(false);

        public Holder lobbyAutojoin = new Holder("lobby-autojoin", "true").setEnabled(false);

        public Holder player = new Holder("player", "Freddiio");
        public Holder playerUuid = new Holder("player-uuid", "c5eb5df7-b7a9-4919-a9bc-7f59c8bee980").setEnabled(false);

        public Placeholder() {
        }
    }

    @ConfigSerializable
    public static class Holder {
        String example;
        String key;
        boolean enabled;
        String placeholder;

        public Holder() {
        }

        public Holder(String key, boolean enabled, String example) {
            this.key = key;
            this.enabled = enabled;
            this.example = example;
        }

        public Holder(String key, String example) {
            this.key = key;
            this.enabled = true;
            this.example = example;
        }

        public Holder setExample(String example) {
            this.example = example;
            return this;
        }

        public String placeholder() {
            return placeholder;
        }

        public Holder setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public String example() {
            return example;
        }

        public String key() {
            return key;
        }

        public Holder setKey(String key) {
            this.key = key;
            return this;
        }

        public boolean enabled() {
            return enabled;
        }

        public Holder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
    }

    @ConfigSerializable
    public static class Lobby {

        public String name;
        public Pattern filter;
        public String permission;
        public int priority;
        public Map<String, Command> commands;
        public boolean autojoin;
        public Messages overwriteMessages = new Messages();

        public Lobby() {
        }

        public Lobby(String name, Pattern filter, String permission, int priority, Map<String, Command> commands, boolean autojoin) {
            this.name = name;
            this.filter = filter;
            this.permission = permission;
            this.priority = priority;
            this.commands = commands;
            this.autojoin = autojoin;
        }

        public String name() {
            return name;
        }

        public Lobby setName(String name) {
            this.name = name;
            return this;
        }

        public Pattern filter() {
            return filter;
        }

        public Lobby setFilter(Pattern filter) {
            this.filter = filter;
            return this;
        }

        public String permission() {
            return permission;
        }

        public Lobby setPermission(String permission) {
            this.permission = permission;
            return this;
        }

        public int priority() {
            return priority;
        }

        public Lobby setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Map<String, Command> commands() {
            return commands;
        }

        public Lobby setCommands(Map<String, Command> commands) {
            this.commands = commands;
            return this;
        }

        public boolean autojoin() {
            return autojoin;
        }

        public Lobby setAutojoin(boolean autojoin) {
            this.autojoin = autojoin;
            return this;
        }

        public Messages messages() {
            return overwriteMessages;
        }

        public Lobby setMessages(Messages messages) {
            this.overwriteMessages = messages;
            return this;
        }
    }

    @ConfigSerializable
    public static class UpdateChecker {

        public boolean enabled = true;
        public String notification = "hub.update";
        public Integer checkIntervalInMin = 5;
        public String notificationMessage = """
                An update is available! Latest version: <latest>, you are using: <current>
                Download it at https://modrinth.com/plugin/hub/version/<latest>
                """;


        public UpdateChecker() {
        }

        public UpdateChecker(boolean enabled, String notification, Integer checkIntervalInMin) {
            this.enabled = enabled;
            this.notification = notification;
            this.checkIntervalInMin = checkIntervalInMin;

        }

        public boolean enabled() {
            return enabled;
        }

        public UpdateChecker setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public String notification() {
            return notification;
        }

        public UpdateChecker setNotification(String notification) {
            this.notification = notification;
            return this;
        }

        public Integer checkIntervalInMin() {
            return checkIntervalInMin;
        }

        public UpdateChecker setCheckIntervalInMin(Integer checkIntervalInMin) {
            this.checkIntervalInMin = checkIntervalInMin;
            return this;
        }
    }

    @ConfigSerializable
    public static class Command {
        public boolean standalone = false;
        public boolean subcommand = false;
        public Pattern hideOn = Pattern.compile("^(?!.*).$");

        public Command() {
        }

        public Command(boolean standalone, boolean subcommand) {
            this.standalone = standalone;
            this.subcommand = subcommand;
        }

        public boolean standalone() {
            return standalone;
        }

        public Command setStandalone(boolean standalone) {
            this.standalone = standalone;
            return this;
        }

        public boolean subcommand() {
            return subcommand;
        }

        public Command setSubcommand(boolean subcommand) {
            this.subcommand = subcommand;
            return this;
        }

        public Pattern hideOn() {
            return hideOn;
        }

        public Command setHideOn(Pattern hideOn) {
            this.hideOn = hideOn;
            return this;
        }
    }

    @ConfigSerializable
    public static class Messages {
        public String successMessage;
        public String alreadyConnectedMessage;
        public String connectionInProgressMessage;
        public String serverDisconnectedMessage;
        public String connectionCancelledMessage;

        public Messages(String successMessage, String alreadyConnectedMessage, String connectionInProgressMessage, String serverDisconnectedMessage, String connectionCancelledMessage) {
            this.successMessage = successMessage;
            this.alreadyConnectedMessage = alreadyConnectedMessage;
            this.connectionInProgressMessage = connectionInProgressMessage;
            this.serverDisconnectedMessage = serverDisconnectedMessage;
            this.connectionCancelledMessage = connectionCancelledMessage;
        }

        public Messages() {
        }

        public String successMessage() {
            return successMessage;
        }

        public Messages setSuccessMessage(String successMessage) {
            this.successMessage = successMessage;
            return this;
        }

        public String alreadyConnectedMessage() {
            return alreadyConnectedMessage;
        }

        public Messages setAlreadyConnectedMessage(String alreadyConnectedMessage) {
            this.alreadyConnectedMessage = alreadyConnectedMessage;
            return this;
        }

        public String connectionInProgressMessage() {
            return connectionInProgressMessage;
        }

        public Messages setConnectionInProgressMessage(String connectionInProgressMessage) {
            this.connectionInProgressMessage = connectionInProgressMessage;
            return this;
        }

        public String serverDisconnectedMessage() {
            return serverDisconnectedMessage;
        }

        public Messages setServerDisconnectedMessage(String serverDisconnectedMessage) {
            this.serverDisconnectedMessage = serverDisconnectedMessage;
            return this;
        }

        public String connectionCancelledMessage() {
            return connectionCancelledMessage;
        }

        public Messages setConnectionCancelledMessage(String connectionCancelledMessage) {
            this.connectionCancelledMessage = connectionCancelledMessage;
            return this;
        }
    }

    @ConfigSerializable
    public static class SystemMessages {
        public String playersOnlyCommandMessage = "<#ff9c59>This Command is only available to Players.";
        public String noLobbyFoundMessage = "<#ff9c59>I'm sorry! i was unable to find a Lobby Server for you.";

    }

}