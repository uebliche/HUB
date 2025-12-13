package net.uebliche.hub.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.regex.Pattern;

@ConfigSerializable
public class Command {
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