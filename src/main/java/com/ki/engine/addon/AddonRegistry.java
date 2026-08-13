package com.ki.engine.addon;

import com.ki.engine.addon.event.AddonEventBus;
import com.ki.engine.block.KiBlock;
import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.database.DatabaseManager;
import com.ki.engine.enchantment.KiEnchantment;
import com.ki.engine.enchantment.effect.EnchantmentEffect;
import com.ki.engine.entity.KiMob;
import com.ki.engine.item.KiItem;
import com.ki.engine.particle.ParticleEffectData;
import com.ki.engine.particle.ParticleManager;
import com.ki.engine.recipe.KiRecipe;
import com.ki.engine.skill.KiSkill;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 附属注册中心 - 提供统一的扩展注册接口。
 * 附属插件通过此接口向 KiEngine 注册自定义内容，无需直接操作内部 Manager。
 *
 * 支持注册的内容类型：
 *   - 自定义物品 (KiItem)
 *   - 自定义方块 (KiBlock)
 *   - 自定义生物 (KiMob)
 *   - 自定义配方 (KiRecipe)
 *   - 自定义技能 (KiSkill)
 *   - 自定义附魔 (KiEnchantment)
 *   - 自定义附魔效果 (EnchantmentEffect)
 *   - 粒子预设 (ParticleEffectData)
 *   - 事件监听器 (Listener)
 *   - 权限节点 (Permission)
 *   - PAPI 变量 (Placeholder)
 *   - 重载钩子 (Runnable)
 *
 * 基础设施：
 *   - 配置文件读写
 *   - 数据目录访问
 *   - 数据库连接
 *   - 任务调度
 *   - 事件总线
 *   - 版本检查
 */
public class AddonRegistry {

    private final KiEnginePlugin plugin;
    private final AddonEventBus eventBus;
    private final Map<String, List<Runnable>> reloadHooks = new HashMap<>();
    private final Map<String, List<Listener>> addonListeners = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredItems = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredBlocks = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredMobs = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredRecipes = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredSkills = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredEnchantments = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredParticles = new ConcurrentHashMap<>();
    private final Map<String, List<Permission>> addonPermissions = new ConcurrentHashMap<>();
    private final Map<String, List<BukkitTask>> addonTasks = new ConcurrentHashMap<>();
    private final Map<String, String> addonMinVersions = new ConcurrentHashMap<>();
    private final Set<String> registeredPlaceholders = ConcurrentHashMap.newKeySet();

    public AddonRegistry(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.eventBus = new AddonEventBus(plugin);
    }

    // ========== 版本检查 ==========

    /**
     * 声明附属所需的最低 KiEngine 版本
     * @param addonId 附属ID
     * @param minVersion 最低版本号（如 "1.0.0"）
     * @return true 如果当前版本满足要求
     */
    public boolean requireVersion(String addonId, String minVersion) {
        addonMinVersions.put(addonId, minVersion);
        String current = plugin.getDescription().getVersion();
        boolean satisfied = compareVersions(current, minVersion) >= 0;
        if (!satisfied) {
            plugin.getLogger().warning("[AddonRegistry] " + addonId + " requires KiEngine >= " + minVersion + ", but current is " + current);
        }
        return satisfied;
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < parts1.length ? Integer.parseInt(parts1[i].replaceAll("[^0-9]", "")) : 0;
            int n2 = i < parts2.length ? Integer.parseInt(parts2[i].replaceAll("[^0-9]", "")) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    // ========== 自定义物品注册 ==========

    public void registerItem(String addonId, KiItem item) {
        plugin.getItemManager().getRegistry().register(item.getId(), item);
        addonRegisteredItems.computeIfAbsent(addonId, k -> ConcurrentHashMap.newKeySet()).add(item.getId());
        plugin.getLogger().info("[AddonRegistry] [" + addonId + "] Registered item: " + item.getId());
    }

    public void unregisterItem(String addonId, String itemId) {
        plugin.getItemManager().getRegistry().unregister(itemId);
        Set<String> items = addonRegisteredItems.get(addonId);
        if (items != null) items.remove(itemId);
    }

    public void unregisterAllItems(String addonId) {
        Set<String> items = addonRegisteredItems.remove(addonId);
        if (items != null) {
            for (String id : items) {
                plugin.getItemManager().getRegistry().unregister(id);
            }
        }
    }

    // ========== 自定义方块注册 ==========

    public void registerBlock(String addonId, KiBlock block) {
        plugin.getBlockManager().getRegistry().register(block.getId(), block);
        addonRegisteredBlocks.computeIfAbsent(addonId, k -> ConcurrentHashMap.newKeySet()).add(block.getId());
        plugin.getLogger().info("[AddonRegistry] [" + addonId + "] Registered block: " + block.getId());
    }

    public void unregisterBlock(String addonId, String blockId) {
        plugin.getBlockManager().getRegistry().unregister(blockId);
        Set<String> blocks = addonRegisteredBlocks.get(addonId);
        if (blocks != null) blocks.remove(blockId);
    }

    public void unregisterAllBlocks(String addonId) {
        Set<String> blocks = addonRegisteredBlocks.remove(addonId);
        if (blocks != null) {
            for (String id : blocks) {
                plugin.getBlockManager().getRegistry().unregister(id);
            }
        }
    }

    // ========== 自定义生物注册 ==========

    public void registerMob(String addonId, KiMob mob) {
        plugin.getEntityManager().getMobRegistry().register(mob.getId(), mob);
        addonRegisteredMobs.computeIfAbsent(addonId, k -> ConcurrentHashMap.newKeySet()).add(mob.getId());
        plugin.getLogger().info("[AddonRegistry] [" + addonId + "] Registered mob: " + mob.getId());
    }

    public void unregisterMob(String addonId, String mobId) {
        plugin.getEntityManager().getMobRegistry().unregister(mobId);
        Set<String> mobs = addonRegisteredMobs.get(addonId);
        if (mobs != null) mobs.remove(mobId);
    }

    public void unregisterAllMobs(String addonId) {
        Set<String> mobs = addonRegisteredMobs.remove(addonId);
        if (mobs != null) {
            for (String id : mobs) {
                plugin.getEntityManager().getMobRegistry().unregister(id);
            }
        }
    }

    // ========== 自定义配方注册 ==========

    public void registerRecipe(String addonId, KiRecipe recipe) {
        plugin.getRecipeManager().getRegistry().register(recipe.getId(), recipe);
        addonRegisteredRecipes.computeIfAbsent(addonId, k -> ConcurrentHashMap.newKeySet()).add(recipe.getId());
        plugin.getLogger().info("[AddonRegistry] [" + addonId + "] Registered recipe: " + recipe.getId());
    }

    public void unregisterRecipe(String addonId, String recipeId) {
        plugin.getRecipeManager().getRegistry().unregister(recipeId);
        Set<String> recipes = addonRegisteredRecipes.get(addonId);
        if (recipes != null) recipes.remove(recipeId);
    }

    public void unregisterAllRecipes(String addonId) {
        Set<String> recipes = addonRegisteredRecipes.remove(addonId);
        if (recipes != null) {
            for (String id : recipes) {
                plugin.getRecipeManager().getRegistry().unregister(id);
            }
        }
    }

    // ========== 自定义技能注册 ==========

    public void registerSkill(String addonId, KiSkill skill) {
        plugin.getSkillManager().getRegistry().register(skill.getId(), skill);
        addonRegisteredSkills.computeIfAbsent(addonId, k -> ConcurrentHashMap.newKeySet()).add(skill.getId());
        plugin.getLogger().info("[AddonRegistry] [" + addonId + "] Registered skill: " + skill.getId());
    }

    public void unregisterSkill(String addonId, String skillId) {
        plugin.getSkillManager().getRegistry().unregister(skillId);
        Set<String> skills = addonRegisteredSkills.get(addonId);
        if (skills != null) skills.remove(skillId);
    }

    public void unregisterAllSkills(String addonId) {
        Set<String> skills = addonRegisteredSkills.remove(addonId);
        if (skills != null) {
            for (String id : skills) {
                plugin.getSkillManager().getRegistry().unregister(id);
            }
        }
    }

    // ========== 自定义附魔注册 ==========

    public void registerEnchantment(String addonId, KiEnchantment enchantment) {
        plugin.getEnchantmentManager().getRegistry().register(enchantment.getId(), enchantment);
        addonRegisteredEnchantments.computeIfAbsent(addonId, k -> ConcurrentHashMap.newKeySet()).add(enchantment.getId());
        plugin.getLogger().info("[AddonRegistry] [" + addonId + "] Registered enchantment: " + enchantment.getId());
    }

    public void unregisterEnchantment(String addonId, String enchantId) {
        plugin.getEnchantmentManager().getRegistry().unregister(enchantId);
        Set<String> enchants = addonRegisteredEnchantments.get(addonId);
        if (enchants != null) enchants.remove(enchantId);
    }

    public void unregisterAllEnchantments(String addonId) {
        Set<String> enchants = addonRegisteredEnchantments.remove(addonId);
        if (enchants != null) {
            for (String id : enchants) {
                plugin.getEnchantmentManager().getRegistry().unregister(id);
            }
        }
    }

    // ========== 附魔效果注册 ==========

    public void registerEnchantmentEffect(EnchantmentEffect effect) {
        plugin.getEnchantmentManager().registerEffect(effect);
    }

    // ========== 粒子预设注册 ==========

    public void registerParticlePreset(String addonId, String presetId, ParticleEffectData data) {
        ParticleManager pm = plugin.getParticleManager();
        if (pm != null) {
            pm.registerPreset(presetId, data);
            addonRegisteredParticles.computeIfAbsent(addonId, k -> ConcurrentHashMap.newKeySet()).add(presetId);
        }
    }

    public void unregisterAllParticles(String addonId) {
        Set<String> presets = addonRegisteredParticles.remove(addonId);
        if (presets != null) {
            ParticleManager pm = plugin.getParticleManager();
            if (pm != null) {
                for (String id : presets) pm.unregisterPreset(id);
            }
        }
    }

    // ========== 权限节点注册 ==========

    /**
     * 为附属注册权限节点
     * @param addonId 附属ID
     * @param name 权限名称（如 "myaddon.use"）
     * @param description 权限描述
     * @param defaultValue 默认值
     * @param children 子权限
     */
    public void registerPermission(String addonId, String name, String description, PermissionDefault defaultValue, Map<String, Boolean> children) {
        Permission perm = new Permission(name, description, defaultValue, children);
        plugin.getServer().getPluginManager().addPermission(perm);
        addonPermissions.computeIfAbsent(addonId, k -> new ArrayList<>()).add(perm);
    }

    public void registerPermission(String addonId, String name, String description, PermissionDefault defaultValue) {
        registerPermission(addonId, name, description, defaultValue, null);
    }

    public void unregisterAllPermissions(String addonId) {
        List<Permission> perms = addonPermissions.remove(addonId);
        if (perms != null) {
            for (Permission perm : perms) {
                plugin.getServer().getPluginManager().removePermission(perm);
            }
        }
    }

    // ========== PAPI 变量注册 ==========

    /**
     * 注册 PlaceholderAPI 变量（需要 PlaceholderAPI 已安装）
     * @param identifier 变量标识符（如 "myaddon" -> %myaddon_value%）
     * @param handler 变量处理函数
     */
    public void registerPlaceholder(String addonId, String identifier, PlaceholderHandler handler) {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().warning("[AddonRegistry] PlaceholderAPI not installed, skipping placeholder: " + identifier);
            return;
        }
        // Placeholder registration requires KiEngineExpansion support
        // For now, just log the registration
        registeredPlaceholders.add(addonId + ":" + identifier);
        plugin.getLogger().info("[AddonRegistry] [" + addonId + "] Registered placeholder: %" + identifier + "_xxx%");
    }

    public void unregisterAllPlaceholders(String addonId) {
        registeredPlaceholders.removeIf(p -> p.startsWith(addonId + ":"));
    }

    @FunctionalInterface
    public interface PlaceholderHandler {
        String onRequest(org.bukkit.entity.Player player, String params);
    }

    // ========== 事件监听器注册 ==========

    public void registerListener(String addonId, Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        addonListeners.computeIfAbsent(addonId, k -> new ArrayList<>()).add(listener);
    }

    public void unregisterAllListeners(String addonId) {
        addonListeners.remove(addonId);
    }

    // ========== 事件总线 ==========

    public AddonEventBus getEventBus() {
        return eventBus;
    }

    // ========== 调度器 API ==========

    /**
     * 执行同步任务
     */
    public BukkitTask runTask(String addonId, Runnable task) {
        BukkitTask t = plugin.getScheduler().run(task);
        addonTasks.computeIfAbsent(addonId, k -> new ArrayList<>()).add(t);
        return t;
    }

    /**
     * 延迟执行同步任务
     */
    public BukkitTask runTaskLater(String addonId, Runnable task, long ticks) {
        BukkitTask t = plugin.getScheduler().runLater(task, ticks);
        addonTasks.computeIfAbsent(addonId, k -> new ArrayList<>()).add(t);
        return t;
    }

    /**
     * 定时执行同步任务
     */
    public BukkitTask runTaskTimer(String addonId, Runnable task, long delay, long period) {
        BukkitTask t = plugin.getScheduler().runTimer(task, delay, period);
        addonTasks.computeIfAbsent(addonId, k -> new ArrayList<>()).add(t);
        return t;
    }

    /**
     * 执行异步任务
     */
    public BukkitTask runAsync(String addonId, Runnable task) {
        BukkitTask t = plugin.getScheduler().runAsync(task);
        addonTasks.computeIfAbsent(addonId, k -> new ArrayList<>()).add(t);
        return t;
    }

    /**
     * 定时执行异步任务
     */
    public BukkitTask runAsyncTimer(String addonId, Runnable task, long delay, long period) {
        BukkitTask t = plugin.getScheduler().runAsyncTimer(task, delay, period);
        addonTasks.computeIfAbsent(addonId, k -> new ArrayList<>()).add(t);
        return t;
    }

    public void cancelAllTasks(String addonId) {
        List<BukkitTask> tasks = addonTasks.remove(addonId);
        if (tasks != null) {
            for (BukkitTask t : tasks) {
                if (!t.isCancelled()) t.cancel();
            }
        }
    }

    // ========== 重载钩子 ==========

    public void registerReloadHook(String addonId, Runnable hook) {
        reloadHooks.computeIfAbsent(addonId, k -> new ArrayList<>()).add(hook);
    }

    public void executeReloadHooks() {
        for (List<Runnable> hooks : reloadHooks.values()) {
            for (Runnable hook : hooks) {
                try { hook.run(); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    // ========== 附属配置 ==========

    public YamlConfiguration getAddonConfig(String addonId) {
        File configFile = new File(getAddonDataFolder(addonId), "config.yml");
        if (!configFile.exists()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveAddonConfig(String addonId, YamlConfiguration config) {
        File configFile = new File(getAddonDataFolder(addonId), "config.yml");
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().warning("[AddonRegistry] Failed to save config for " + addonId + ": " + e.getMessage());
        }
    }

    /**
     * 注册附属配置到 KiEngine 重载系统
     * 当 /ki reload 执行时，会自动重新加载此配置
     */
    public void registerReloadableConfig(String addonId, String configName, ReloadableConfig handler) {
        registerReloadHook(addonId, () -> {
            File configFile = new File(getAddonDataFolder(addonId), configName);
            if (configFile.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                handler.onReload(config);
            }
        });
    }

    @FunctionalInterface
    public interface ReloadableConfig {
        void onReload(YamlConfiguration config);
    }

    // ========== 数据目录 ==========

    public File getAddonDataFolder(String addonId) {
        File dir = new File(plugin.getDataFolder(), "addons/" + addonId);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    // ========== 数据库访问 ==========

    public Connection getDatabaseConnection() throws SQLException {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) throw new SQLException("DatabaseManager not initialized");
        return db.getConnection();
    }

    public int executeUpdate(String sql, Object... params) throws SQLException {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) throw new SQLException("DatabaseManager not initialized");
        return db.executeUpdate(sql, params);
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) throw new SQLException("DatabaseManager not initialized");
        return db.executeQuery(sql, params);
    }

    // ========== 统计信息 ==========

    public Map<String, Integer> getAddonStats(String addonId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("items", addonRegisteredItems.getOrDefault(addonId, Set.of()).size());
        stats.put("blocks", addonRegisteredBlocks.getOrDefault(addonId, Set.of()).size());
        stats.put("mobs", addonRegisteredMobs.getOrDefault(addonId, Set.of()).size());
        stats.put("recipes", addonRegisteredRecipes.getOrDefault(addonId, Set.of()).size());
        stats.put("skills", addonRegisteredSkills.getOrDefault(addonId, Set.of()).size());
        stats.put("enchantments", addonRegisteredEnchantments.getOrDefault(addonId, Set.of()).size());
        stats.put("particles", addonRegisteredParticles.getOrDefault(addonId, Set.of()).size());
        stats.put("permissions", addonPermissions.getOrDefault(addonId, List.of()).size());
        stats.put("tasks", addonTasks.getOrDefault(addonId, List.of()).size());
        return stats;
    }

    public Set<String> getRegisteredAddonIds() {
        Set<String> ids = new HashSet<>();
        ids.addAll(addonRegisteredItems.keySet());
        ids.addAll(addonRegisteredBlocks.keySet());
        ids.addAll(addonRegisteredMobs.keySet());
        ids.addAll(addonRegisteredRecipes.keySet());
        ids.addAll(addonRegisteredSkills.keySet());
        ids.addAll(addonRegisteredEnchantments.keySet());
        return ids;
    }

    // ========== 快捷访问 ==========

    public KiEnginePlugin getPlugin() { return plugin; }

    /**
     * 卸载附属的所有注册内容
     */
    public void unregisterAll(String addonId) {
        unregisterAllItems(addonId);
        unregisterAllBlocks(addonId);
        unregisterAllMobs(addonId);
        unregisterAllRecipes(addonId);
        unregisterAllSkills(addonId);
        unregisterAllEnchantments(addonId);
        unregisterAllParticles(addonId);
        unregisterAllPermissions(addonId);
        unregisterAllPlaceholders(addonId);
        unregisterAllListeners(addonId);
        cancelAllTasks(addonId);
        eventBus.unsubscribeAll(addonId);
        reloadHooks.remove(addonId);
        addonMinVersions.remove(addonId);
        plugin.getLogger().info("[AddonRegistry] Unregistered all content for: " + addonId);
    }
}
