package com.ki.engine.placeholder;

import com.ki.engine.core.KiEnginePlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KiEngineExpansion extends PlaceholderExpansion {
    private final KiEnginePlugin plugin;

    public KiEngineExpansion(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull public String getIdentifier() { return "ki"; }
    @Override @NotNull public String getAuthor() { return "KiTeam"; }
    @Override @NotNull public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        // Handle total level: %ki_total_level%
        if (params.equalsIgnoreCase("total_level")) {
            int total = 0;
            for (String s : new String[]{"combat","fishing","farming","alchemy","smithing","magic","exploration"}) {
                total += plugin.getRpgManager().getLevel(player, s);
            }
            return String.valueOf(total);
        }

        String[] parts = params.split("_");
        if (parts.length < 2) return "";

        String skill = parts[0].toLowerCase();
        String type = parts[1].toLowerCase();

        return switch (type) {
            case "level" -> String.valueOf(plugin.getRpgManager().getLevel(player, skill));
            case "exp" -> String.valueOf((int) plugin.getRpgManager().getExp(player, skill));
            default -> "";
        };
    }
}
