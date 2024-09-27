package io.freddi.hub.utils;

import io.freddi.hub.Hub;

import java.util.HashMap;

public class Utils<T extends Utils<?>> {

    private static final HashMap<Class<? extends Utils>, Utils> UTILS = new HashMap<>();
    protected final org.slf4j.Logger logger;
    protected final Hub hub;

    public Utils(Hub hub) {
        this.logger = org.slf4j.LoggerFactory.getLogger("hub | " + this.getClass().getSimpleName().toLowerCase());
        this.hub = hub;
        UTILS.put(getClass(), this);
    }


    public static <T> T util(Class<T> utilClass) {
        return (T) UTILS.get(utilClass);
    }

}
