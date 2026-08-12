package com.ki.engine.addon;

import com.ki.engine.block.KiBlock;
import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.database.DatabaseManager;
import com.ki.engine.enchantment.KiEnchantment;
import com.ki.engine.enchantment.effect.EnchantmentEffect;
import com.ki.engine.entity.KiMob;
import com.ki.engine.item.KiItem;
import com.ki.engine.recipe.KiRecipe;
import com.ki.engine.skill.KiSkill;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
 *   - 事件监听器 (Listener)
 *   - 重载钩子 (Runnable)
 *
 * 基础设施：
 *   - 配置文件读写
 *   - 数据目录访问
 *   - 数据库连接
 */
public class AddonRegistry {

    private final KiEnginePlugin plugin;
    private final Map<String, List<Runnable>> reloadHooks = new HashMap<>();
    private final Map<String, List<Listener>> addonListeners = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredItems = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredBlocks = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredMobs = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredRecipes = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredSkills = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> addonRegisteredEnchantments = new ConcurrentHashMap<>();

    public AddonRegistry(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    // ========== 自定义物品注册 ==========

    /**
     * 注册自定义物品
     * @param addonId 附属ID（用于追踪归属和卸载）
     * @param item 物品定义
     */
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

    // ========== 事件监听器注册 ==========

    /**
     * 为附属注册 Bukkit 事件监听器
     */
    public void registerListener(String addonId, Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        addonListeners.computeIfAbsent(addonId, k -> new ArrayList<>()).add(listener);
    }

    public void unregisterAllListeners(String addonId) {
        addonListeners.remove(addonId);
        // Bukkit 没有 unregisterEvents 的单方法，需要 HandlerList 处理
        // 这里记录即可，实际卸载由 Bukkit 在插件禁用时自动清理
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

    /**
     * 获取附属自己的配置文件 (addons/<addonId>/config.yml)
     */
    public YamlConfiguration getAddonConfig(String addonId) {
        File configFile = new File(getAddonDataFolder(addonId), "config.yml");
        if (!configFile.exists()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * 保存附属配置文件
     */
    public void saveAddonConfig(String addonId, YamlConfiguration config) {
        File configFile = new File(getAddonDataFolder(addonId), "config.yml");
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().warning("[AddonRegistry] Failed to save config for " + addonId + ": " + e.getMessage());
        }
    }

    // ========== 数据目录 ==========

    /**
     * 获取附属数据目录 (plugins/KiEngine/addons/<addonId>/)
     */
    public File getAddonDataFolder(String addonId) {
        File dir = new File(plugin.getDataFolder(), "addons/" + addonId);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    // ========== 数据库访问 ==========

    /**
     * 获取数据库连接（供附属插件执行自定义 SQL）
     */
    public Connection getDatabaseConnection() throws SQLException {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) throw new SQLException("DatabaseManager not initialized");
        return db.getConnection();
    }

    /**
     * 执行数据库更新（INSERT/UPDATE/DELETE）
     */
    public int executeUpdate(String sql, Object... params) throws SQLException {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) throw new SQLException("DatabaseManager not initialized");
        return db.executeUpdate(sql, params);
    }

    /**
     * 执行数据库查询
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) throw new SQLException("DatabaseManager not initialized");
        return db.executeQuery(sql, params);
    }

    // ========== 快捷访问 ==========

    public KiEnginePlugin getPlugin() { return plugin; }

    /**
     * 卸载附属的所有注册内容（物品、方块、生物、配方、技能、附魔）
     */
    public void unregisterAll(String addonId) {
        unregisterAllItems(addonId);
        unregisterAllBlocks(addonId);
        unregisterAllMobs(addonId);
        unregisterAllRecipes(addonId);
        unregisterAllSkills(addonId);
        unregisterAllEnchantments(addonId);
        unregisterAllListeners(addonId);
        reloadHooks.remove(addonId);
        plugin.getLogger().info("[AddonRegistry] Unregistered all content for: " + addonId);
    }
}
