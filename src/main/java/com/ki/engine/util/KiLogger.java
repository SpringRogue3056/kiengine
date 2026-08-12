package com.ki.engine.util;

import com.ki.engine.core.KiEnginePlugin;
import java.util.logging.Level;

/**
 * Unified logging facade with debug levels and component tagging.
 */
public class KiLogger {
    private final KiEnginePlugin plugin;
    private boolean debugEnabled = false;

    public KiLogger(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
    }

    public void info(String component, String msg) {
        plugin.getLogger().info("[" + component + "] " + msg);
    }

    public void warn(String component, String msg) {
        plugin.getLogger().warning("[" + component + "] " + msg);
    }

    public void error(String component, String msg) {
        plugin.getLogger().severe("[" + component + "] " + msg);
    }

    public void error(String component, String msg, Throwable t) {
        plugin.getLogger().log(Level.SEVERE, "[" + component + "] " + msg, t);
    }

    public void debug(String component, String msg) {
        if (debugEnabled) {
            plugin.getLogger().info("[DEBUG][" + component + "] " + msg);
        }
    }
}
