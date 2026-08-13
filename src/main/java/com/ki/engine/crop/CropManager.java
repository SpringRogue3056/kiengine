package com.ki.engine.crop;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CropManager implements Manager {

    private final KiEnginePlugin plugin;
    private final Map<String, CropData> crops = new ConcurrentHashMap<>();
    private final Map<String, String> cropLocations = new ConcurrentHashMap<>();
    private final File locFile;

    public CropManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.locFile = new File(plugin.getDataFolder(), "data/crops_locations.yml");
    }

    @Override
    public void init() {
        reload();
    }

    @Override
    public void reload() {
        crops.clear();
        cropLocations.clear();
        Map<String, YamlConfiguration> configs = plugin.getConfigManager().getConfigsByType("crops");
        for (YamlConfiguration config : configs.values()) {
            loadCrops(config);
        }
        loadLocations();
        plugin.getLogger().info("[CropManager] Loaded " + crops.size() + " custom crops");
    }

    private void loadCrops(YamlConfiguration config) {
        for (String key : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(key);
            if (sec == null) continue;
            try {
                String seedId = sec.getString("seed", key).toLowerCase();
                Material baseBlock = Material.valueOf(sec.getString("base_block", "WHEAT").toUpperCase());
                int growthStages = sec.getInt("growth_stages", 7);
                String matureDrop = sec.getString("mature_drop", key);
                String seedDrop = sec.getString("seed_drop", seedId);
                double seedReturnChance = sec.getDouble("seed_return_chance", 0.7);
                int minDrop = sec.getInt("min_drop", 1);
                int maxDrop = sec.getInt("max_drop", 3);
                CropData data = new CropData(seedId, baseBlock, growthStages, matureDrop, seedDrop, seedReturnChance, minDrop, maxDrop);
                crops.put(seedId, data);
            } catch (Exception e) {
                plugin.getLogger().warning("[CropManager] Failed to load crop: " + key);
            }
        }
    }

    private void loadLocations() {
        if (!locFile.exists()) return;
        YamlConfiguration c = YamlConfiguration.loadConfiguration(locFile);
        for (String k : c.getKeys(false)) {
            cropLocations.put(k, c.getString(k));
        }
    }

    public void saveLocations() {
        try {
            locFile.getParentFile().mkdirs();
            YamlConfiguration c = new YamlConfiguration();
            for (Map.Entry<String, String> e : cropLocations.entrySet()) c.set(e.getKey(), e.getValue());
            c.save(locFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[CropManager] Save locations failed: " + e.getMessage());
        }
    }

    public void setCropLocation(Location loc, String cropId) {
        cropLocations.put(locToKey(loc), cropId);
        saveLocations();
    }

    public String getCropAt(Location loc) {
        return cropLocations.get(locToKey(loc));
    }

    public void removeCropLocation(Location loc) {
        cropLocations.remove(locToKey(loc));
        saveLocations();
    }

    private String locToKey(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    public CropData getCrop(String seedId) {
        return crops.get(seedId.toLowerCase());
    }

    public boolean isCustomSeed(String itemId) {
        return crops.containsKey(itemId.toLowerCase());
    }

    public Collection<CropData> getAllCrops() {
        return crops.values();
    }

    public record CropData(String seedId, Material baseBlock, int growthStages,
                           String matureDrop, String seedDrop, double seedReturnChance,
                           int minDrop, int maxDrop) {}
}
