package com.ki.engine.addon;

import com.ki.engine.addon.annotation.KiAddonMeta;
import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 附属插件管理器 - 自动扫描 plugins/KiEngine/addons/ 目录下的 jar 文件，
 * 加载标注了 @KiAddonMeta 的 KiAddon 实现类。
 */
public class AddonManager implements Manager {

    private final KiEnginePlugin plugin;
    private final File addonsDir;
    private final List<LoadedAddon> addons = new ArrayList<>();
    private final Map<String, URLClassLoader> loaders = new HashMap<>();

    public AddonManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.addonsDir = new File(plugin.getDataFolder(), "addons");
    }

    @Override
    public void init() {
        // onEnable 阶段再加载 addon
    }

    /**
     * 在 KiEngine 所有 Manager 初始化完成后调用
     */
    public void loadAddons() {
        if (!addonsDir.exists()) {
            addonsDir.mkdirs();
            plugin.getLogger().info("[AddonManager] Created addons directory: " + addonsDir.getPath());
        }

        File[] jars = addonsDir.listFiles(f -> f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            plugin.getLogger().info("[AddonManager] No addons found.");
            return;
        }

        // Create config directories for each addon jar
        for (File jar : jars) {
            String addonName = jar.getName().replace(".jar", "");
            new File(addonsDir, addonName).mkdirs();
        }

        for (File jar : jars) {
            try {
                loadAddonJar(jar);
            } catch (Exception e) {
                plugin.getLogger().warning("[AddonManager] Failed to load " + jar.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("[AddonManager] Loaded " + addons.size() + " addon(s).");

        // Phase 0.5: Check addon-to-addon dependencies
        Set<String> loadedAddonIds = addons.stream().map(a -> a.meta.id()).collect(java.util.stream.Collectors.toSet());
        for (LoadedAddon addon : addons) {
            for (String dep : addon.meta.depend()) {
                if (!loadedAddonIds.contains(dep) && plugin.getServer().getPluginManager().getPlugin(dep) == null) {
                    plugin.getLogger().warning("[AddonManager] " + addon.meta.id() + " depends on addon/plugin '" + dep + "' but not found.");
                }
            }
            for (String softDep : addon.meta.softDepend()) {
                if (!loadedAddonIds.contains(softDep) && plugin.getServer().getPluginManager().getPlugin(softDep) == null) {
                    plugin.getLogger().warning("[AddonManager] " + addon.meta.id() + " soft-depends on '" + softDep + "' but not found.");
                }
            }
        }

        // Phase 1: onLoad
        for (LoadedAddon addon : addons) {
            try {
                addon.instance.onLoad(plugin);
                plugin.getLogger().info("[AddonManager] " + addon.meta.id() + " -> onLoad()");
            } catch (Exception e) {
                plugin.getLogger().warning("[AddonManager] " + addon.meta.id() + " onLoad() failed: " + e.getMessage());
            }
        }

        // Phase 2: onEnable
        for (LoadedAddon addon : addons) {
            try {
                addon.instance.onEnable(plugin);
                plugin.getLogger().info("[AddonManager] " + addon.meta.id() + " -> onEnable()");
            } catch (Exception e) {
                plugin.getLogger().warning("[AddonManager] " + addon.meta.id() + " onEnable() failed: " + e.getMessage());
            }
        }
    }

    private void loadAddonJar(File jar) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, plugin.getClass().getClassLoader());
        loaders.put(jar.getName(), classLoader);

        JarFile jarFile = new JarFile(jar);
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!name.endsWith(".class")) continue;

            String className = name.replace('/', '.').replace(".class", "");
            try {
                Class<?> clazz = Class.forName(className, true, classLoader);
                if (!KiAddon.class.isAssignableFrom(clazz)) continue;

                KiAddonMeta meta = clazz.getAnnotation(KiAddonMeta.class);
                if (meta == null) {
                    plugin.getLogger().warning("[AddonManager] " + className + " implements KiAddon but missing @KiAddonMeta, skipping.");
                    continue;
                }

                // Check dependencies
                for (String dep : meta.depend()) {
                    if (plugin.getServer().getPluginManager().getPlugin(dep) == null) {
                        plugin.getLogger().warning("[AddonManager] " + meta.id() + " requires plugin '" + dep + "' but not found, skipping.");
                        return;
                    }
                }

                // Check soft dependencies (warn only)
                for (String softDep : meta.softDepend()) {
                    if (plugin.getServer().getPluginManager().getPlugin(softDep) == null) {
                        plugin.getLogger().warning("[AddonManager] " + meta.id() + " soft-depends on '" + softDep + "' but not found.");
                    }
                }

                KiAddon instance = (KiAddon) clazz.getDeclaredConstructor().newInstance();
                addons.add(new LoadedAddon(meta, instance, classLoader));
                plugin.getLogger().info("[AddonManager] Discovered addon: " + meta.id() + " v" + meta.version() + " by " + String.join(", ", meta.authors()));
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // Dependency class not available, skip
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().warning("[AddonManager] Failed to instantiate " + className + ": " + e.getMessage());
            }
        }
        jarFile.close();
    }

    public void reloadAddons() {
        for (LoadedAddon addon : addons) {
            try {
                addon.instance.onReload(plugin);
            } catch (Exception e) {
                plugin.getLogger().warning("[AddonManager] " + addon.meta.id() + " reload failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void shutdown() {
        // Unregister all content from addon registry
        if (plugin.getAddonRegistry() != null) {
            for (LoadedAddon addon : addons) {
                plugin.getAddonRegistry().unregisterAll(addon.meta.id());
            }
        }

        for (int i = addons.size() - 1; i >= 0; i--) {
            LoadedAddon addon = addons.get(i);
            try {
                addon.instance.onDisable(plugin);
            } catch (Exception e) {
                plugin.getLogger().warning("[AddonManager] " + addon.meta.id() + " onDisable() error: " + e.getMessage());
            }
        }
        addons.clear();
        for (URLClassLoader loader : loaders.values()) {
            try { loader.close(); } catch (IOException ignored) {}
        }
        loaders.clear();
    }

    public List<LoadedAddon> getAddons() { return Collections.unmodifiableList(addons); }

    public record LoadedAddon(KiAddonMeta meta, KiAddon instance, URLClassLoader classLoader) {}
}
