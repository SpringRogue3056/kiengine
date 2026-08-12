package com.ki.engine.block;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockDataStorage {
    private final KiEnginePlugin plugin;
    private final File dataFile;
    private final Map<String, String> blockMap = new ConcurrentHashMap<>();

    public BlockDataStorage(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/blocks.yml");
        load();
    }

    public void setBlock(Location loc, String blockId) {
        blockMap.put(locToKey(loc), blockId);
        save();
    }

    public String getBlock(Location loc) {
        return blockMap.get(locToKey(loc));
    }

    public void removeBlock(Location loc) {
        blockMap.remove(locToKey(loc));
        save();
    }

    public boolean hasBlock(Location loc) {
        return blockMap.containsKey(locToKey(loc));
    }

    private String locToKey(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration c = YamlConfiguration.loadConfiguration(dataFile);
        for (String k : c.getKeys(false)) blockMap.put(k, c.getString(k));
    }

    public void save() {
        try {
            dataFile.getParentFile().mkdirs();
            YamlConfiguration c = new YamlConfiguration();
            for (Map.Entry<String, String> e : blockMap.entrySet()) c.set(e.getKey(), e.getValue());
            c.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[BlockDataStorage] Save failed: " + e.getMessage());
        }
    }
}
