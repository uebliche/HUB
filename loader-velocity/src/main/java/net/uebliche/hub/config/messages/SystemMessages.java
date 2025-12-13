package net.uebliche.hub.config.messages;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class SystemMessages {
    public String playersOnlyCommandMessage = "<#ff9c59>This Command is only available to Players.";
    public String noLobbyFoundMessage = "<#ff9c59>I'm sorry! i was unable to find a Lobby Server for you.";

}