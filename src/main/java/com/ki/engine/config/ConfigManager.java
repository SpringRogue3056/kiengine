package com.ki.engine.config;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Unified config manager with type-indexed cache, async reload, and file watching.
 */
public class ConfigManager {
    private final KiEnginePlugin plugin;
    private final File dataFolder;
    private final Map<String, YamlConfiguration> configs = new ConcurrentHashMap<>();
    /** Type-indexed cache: "items" -> { "file1": config, "file2": config } */
    private final Map<String, Map<String, YamlConfiguration>> typeIndex = new ConcurrentHashMap<>();
    /** World-specific configs: worldName -> { "items" -> { "file1": config } } */
    private final Map<String, Map<String, Map<String, YamlConfiguration>>> worldConfigs = new ConcurrentHashMap<>();
    private WatchService watchService;
    private org.bukkit.scheduler.BukkitTask watchTask;
    private static final ExecutorService RELOAD_EXECUTOR = Executors.newSingleThreadExecutor(
        r -> {
            Thread t = new Thread(r, "KiEngine-ConfigReloader");
            t.setDaemon(true);
            return t;
        }
    );

    public ConfigManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public void loadAll() {
        plugin.getLogger().info("[ConfigManager] Loading configs...");
        long start = System.currentTimeMillis();
        configs.clear();
        typeIndex.clear();
        worldConfigs.clear();
        loadRootConfigs();
        loadContentPacks();
        loadWorldPacks();
        buildTypeIndex();
        startFileWatcher();
        long elapsed = System.currentTimeMillis() - start;
        plugin.getLogger().info("[ConfigManager] Loaded " + configs.size() + " global + " + worldConfigs.size() + " world-specific configs in " + elapsed + "ms");
    }

    private void loadRootConfigs() {
        File[] files = dataFolder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String key = file.getName().replace(".yml", "");
                configs.put(key, config);
            } catch (Exception e) {
                plugin.getLogger().warning("[ConfigManager] Failed to load root config " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private void loadContentPacks() {
        File packsDir = new File(dataFolder, "packs");
        if (!packsDir.exists()) {
            packsDir.mkdirs();
            createDefaultPack(packsDir);
        }
        File[] packs = packsDir.listFiles(File::isDirectory);
        if (packs == null) return;
        for (File pack : packs) {
            loadPack(pack);
        }
    }

    private void loadPack(File packDir) {
        String[] types = {"items", "blocks", "entities", "npcs", "recipes", "skills", "crops", "fishing"};
        for (String type : types) {
            loadYamlDir(new File(packDir, type), type);
        }
    }

    private void loadWorldPacks() {
        File worldsDir = new File(dataFolder, "worlds");
        if (!worldsDir.exists()) return;
        File[] worldDirs = worldsDir.listFiles(File::isDirectory);
        if (worldDirs == null) return;
        for (File worldDir : worldDirs) {
            String worldName = worldDir.getName();
            Map<String, Map<String, YamlConfiguration>> worldTypeMap = new ConcurrentHashMap<>();
            String[] types = {"items", "blocks", "entities", "npcs", "recipes", "skills", "crops", "fishing"};
            for (String type : types) {
                File typeDir = new File(worldDir, type);
                if (!typeDir.exists()) continue;
                File[] files = typeDir.listFiles((d, name) -> name.endsWith(".yml"));
                if (files == null) continue;
                Map<String, YamlConfiguration> typeConfigs = new ConcurrentHashMap<>();
                for (File file : files) {
                    try {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        String name = file.getName().replace(".yml", "");
                        typeConfigs.put(name, config);
                    } catch (Exception e) {
                        plugin.getLogger().warning("[ConfigManager] Failed to load world config " + worldName + "/" + file.getName() + ": " + e.getMessage());
                    }
                }
                if (!typeConfigs.isEmpty()) {
                    worldTypeMap.put(type, typeConfigs);
                }
            }
            if (!worldTypeMap.isEmpty()) {
                worldConfigs.put(worldName, worldTypeMap);
                plugin.getLogger().info("[ConfigManager] Loaded world pack: " + worldName + " (" + worldTypeMap.size() + " types)");
            }
        }
    }

    private void loadYamlDir(File dir, String type) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String key = type + ":" + file.getName().replace(".yml", "");
                configs.put(key, config);
            } catch (Exception e) {
                plugin.getLogger().warning("[ConfigManager] Failed to load " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /** Build O(1) type-indexed cache after all configs loaded */
    private void buildTypeIndex() {
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            String key = entry.getKey();
            int colon = key.indexOf(':');
            if (colon < 0) continue;
            String type = key.substring(0, colon);
            String name = key.substring(colon + 1);
            typeIndex.computeIfAbsent(type, k -> new HashMap<>()).put(name, entry.getValue());
        }
    }

    /** O(1) type lookup via cached index */
    public Map<String, YamlConfiguration> getConfigsByType(String type) {
        Map<String, YamlConfiguration> result = typeIndex.get(type);
        return result != null ? new HashMap<>(result) : Collections.emptyMap();
    }

    /**
     * 获取指定世界的配置，合并全局配置 + 世界覆盖配置
     * 世界配置优先级高于全局配置
     */
    public Map<String, YamlConfiguration> getConfigsByType(String type, String worldName) {
        Map<String, YamlConfiguration> result = new HashMap<>(getConfigsByType(type));
        Map<String, Map<String, YamlConfiguration>> worldTypeMap = worldConfigs.get(worldName);
        if (worldTypeMap != null && worldTypeMap.containsKey(type)) {
            result.putAll(worldTypeMap.get(type));
        }
        return result;
    }

    public YamlConfiguration getConfig(String key) {
        return configs.get(key);
    }

    /**
     * 获取指定世界的配置项，优先查找世界特定配置
     */
    public YamlConfiguration getConfig(String type, String name, String worldName) {
        Map<String, Map<String, YamlConfiguration>> worldTypeMap = worldConfigs.get(worldName);
        if (worldTypeMap != null) {
            Map<String, YamlConfiguration> typeConfigs = worldTypeMap.get(type);
            if (typeConfigs != null && typeConfigs.containsKey(name)) {
                return typeConfigs.get(name);
            }
        }
        return configs.get(type + ":" + name);
    }

    /** Async reload to avoid blocking main thread */
    public void reloadAsync(Runnable onComplete) {
        RELOAD_EXECUTOR.submit(() -> {
            try {
                configs.clear();
                typeIndex.clear();
                worldConfigs.clear();
                loadContentPacks();
                loadWorldPacks();
                buildTypeIndex();
                if (onComplete != null) {
                    Bukkit.getScheduler().runTask(plugin, onComplete);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("[ConfigManager] Async reload failed: " + e.getMessage());
            }
        });
    }

    public void reload() {
        reloadAsync(null);
    }

    /** File system watcher for automatic hot-reload */
    private void startFileWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            File packsDir = new File(dataFolder, "packs");
            if (packsDir.exists()) {
                registerWatchRecursive(packsDir);
            }
            watchTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                WatchKey key;
                while ((key = watchService.poll()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = (Path) event.context();
                        if (changed.toString().endsWith(".yml")) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.getLogger().info("[ConfigManager] Detected change: " + changed);
                                plugin.getCommand("ki").execute(Bukkit.getConsoleSender(), "ki", new String[]{"reload"});
                            });
                        }
                    }
                    key.reset();
                }
            }, 100L, 100L);
        } catch (IOException e) {
            plugin.getLogger().warning("[ConfigManager] File watcher not available: " + e.getMessage());
        }
    }

    private void registerWatchRecursive(File dir) throws IOException {
        dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
        File[] children = dir.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                registerWatchRecursive(child);
            }
        }
    }

    public void shutdown() {
        if (watchTask != null) watchTask.cancel();
        if (watchService != null) {
            try { watchService.close(); } catch (IOException ignored) {}
        }
    }

    private void createDefaultPack(File packsDir) {
        File defaultPack = new File(packsDir, "default");
        defaultPack.mkdirs();
        String[] dirs = {"items", "blocks", "entities", "npcs", "recipes", "skills", "crops", "fishing"};
        for (String d : dirs) new File(defaultPack, d).mkdirs();

        // Create example world-specific pack structure
        File worldsDir = new File(dataFolder, "worlds");
        worldsDir.mkdirs();
        File exampleWorld = new File(worldsDir, "world_nether");
        exampleWorld.mkdirs();
        for (String d : dirs) new File(exampleWorld, d).mkdirs();
    }
}
