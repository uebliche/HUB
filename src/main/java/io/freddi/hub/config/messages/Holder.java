package io.freddi.hub.config.messages;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class Holder {
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