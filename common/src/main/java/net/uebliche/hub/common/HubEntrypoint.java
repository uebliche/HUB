package net.uebliche.hub.common;

/**
 * Shared lifecycle contract for loader entrypoints so each platform can
 * follow the same setup steps while providing platform-specific hooks.
 */
public interface HubEntrypoint {
    /**
    * Load configuration and platform resources.
    */
    void loadConfig();

    /**
    * Register item distribution / inventory preparation.
    */
    void registerItems();

    /**
    * Register UI/menu interactions such as selectors or navigators.
    */
    void registerMenus();

    /**
    * Register gameplay guards like damage/hunger toggles or heal behaviour.
    */
    void registerGameplayGuards();

    /**
    * Register transport such as teleports or server connect.
    */
    void registerTransport();
}
