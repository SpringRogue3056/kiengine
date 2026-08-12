package com.ki.engine.rpg;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RPGManager {
    private final KiEnginePlugin plugin;
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final File dataFile;

    public RPGManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "rpg_data.yml");
        load();
    }

    public int getLevel(Player player, String skill) {
        return getData(player).levels.getOrDefault(skill, 1);
    }

    public double getExp(Player player, String skill) {
        return getData(player).exp.getOrDefault(skill, 0.0);
    }

    public void addExp(Player player, String skill, double amount) {
        PlayerData data = getData(player);
        double current = data.exp.getOrDefault(skill, 0.0);
        int level = data.levels.getOrDefault(skill, 1);
        current += amount;
        double needed = getExpNeeded(level);
        while (current >= needed) {
            current -= needed;
            level++;
            data.levels.put(skill, level);
            player.sendMessage("\u00a7a\u00a7lLevel Up!\u00a7r \u00a77" + skill + " reached level " + level);
        }
        data.exp.put(skill, current);
        save();
    }

    private double getExpNeeded(int level) {
        return 100 * Math.pow(1.5, level - 1);
    }

    private PlayerData getData(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), k -> new PlayerData());
    }

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerData data = new PlayerData();
                org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection(key);
                if (sec != null) {
                    for (String skill : sec.getKeys(false)) {
                        data.levels.put(skill, sec.getInt(skill + ".level", 1));
                        data.exp.put(skill, sec.getDouble(skill + ".exp", 0));
                    }
                }
                players.put(uuid, data);
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            String key = entry.getKey().toString();
            PlayerData data = entry.getValue();
            for (Map.Entry<String, Integer> lv : data.levels.entrySet()) {
                config.set(key + "." + lv.getKey() + ".level", lv.getValue());
                config.set(key + "." + lv.getKey() + ".exp", data.exp.getOrDefault(lv.getKey(), 0.0));
            }
        }
        try { config.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("Failed to save RPG data: " + e.getMessage());
        }
    }

    public void reload() {
        players.clear();
        load();
        plugin.getLogger().info("[RPG] Reloaded " + players.size() + " players");
    }

    private static class PlayerData {
        final Map<String, Integer> levels = new HashMap<>();
        final Map<String, Double> exp = new HashMap<>();
    }
}
