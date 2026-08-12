package com.ki.engine.core;

/**
 * Manager lifecycle interface.
 * All managers should implement this for proper initialization and shutdown ordering.
 */
public interface Manager {
    /** Called during INIT phase, after construction but before listeners/commands */
    default void init() {}

    /** Called during shutdown, in reverse registration order */
    default void shutdown() {}

    /** Called during /ki reload */
    default void reload() {}
}
