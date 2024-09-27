package io.freddi.hub.utils;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.Player;
import io.freddi.hub.Hub;
import io.freddi.hub.commands.DebugCommand;
import io.freddi.hub.commands.HubCommand;

import java.util.concurrent.ConcurrentLinkedDeque;

public class CommandUtils extends Utils<CommandUtils> {

    private final ConcurrentLinkedDeque<CommandMeta> commands = new ConcurrentLinkedDeque<>();

    public CommandUtils(Hub hub) {
        super(hub);
        Utils.util(ConfigUtils.class).onReload(() -> {
            unregisterCommands();
            registerCommands();
        });
        if (Utils.util(ConfigUtils.class).config() == null) {
            registerDebugCommand();
            logger.info("Debug Command Registered! (/hub debug reload to reload the config)");
        }
        registerCommands();
    }

    public void unregisterCommands() {
        commands.forEach(this::unregister);
    }

    private void unregister(CommandMeta meta) {
        hub.server().getCommandManager().unregister(meta);
        commands.remove(meta);
    }

    public void registerCommand(CommandMeta meta, BrigadierCommand command) {
        commands.add(meta);
        hub.server().getCommandManager().register(meta, command);
    }

    private void registerCommands() {
        new HubCommand(hub).create();
    }

    private void registerDebugCommand() {
        logger.warn("Debug Command got registered!");
        System.out.println("DEBUG COMMAND REGISTERED!");
        registerCommand(
                hub.server().getCommandManager().metaBuilder("hub").plugin(hub).build(),
                new BrigadierCommand(
                        BrigadierCommand.literalArgumentBuilder("hub")
                                .executes(commandContext -> {
                                    if (!(commandContext.getSource() instanceof Player player)) {
                                        commandContext.getSource().sendMessage(Utils.util(MessageUtils.class).toMessage(Utils.util(ConfigUtils.class).config().systemMessages.playersOnlyCommandMessage));
                                    } else {
                                        hub.server().getConfiguration().getAttemptConnectionOrder().stream().findAny().ifPresent(attemptConnectionOrder -> {
                                            player.createConnectionRequest(hub.server().getServer(attemptConnectionOrder).get()).connect().thenAccept(connection -> {

                                            });
                                        });
                                    }
                                    return 1;
                                })
                                .then(new DebugCommand(hub).create())
                                .build()
                )
        );
    }
}
