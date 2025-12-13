package net.uebliche.hub.config.messages;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class Messages {
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