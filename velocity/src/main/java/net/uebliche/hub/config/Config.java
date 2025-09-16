package net.uebliche.hub.config;

import net.uebliche.hub.config.messages.Holder;
import net.uebliche.hub.config.messages.Messages;
import net.uebliche.hub.config.messages.SystemMessages;
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
        public int refreshIntervalInTicks = 40;

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


}
