package io.freddi.hub.config;

import io.freddi.hub.config.messages.Messages;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Map;
import java.util.regex.Pattern;

@ConfigSerializable
public class Lobby {

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