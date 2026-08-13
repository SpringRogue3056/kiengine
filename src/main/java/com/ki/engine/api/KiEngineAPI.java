package com.ki.engine.api;

import com.ki.engine.addon.AddonManager;
import com.ki.engine.addon.AddonRegistry;
import com.ki.engine.block.BlockManager;
import com.ki.engine.block.KiBlock;
import com.ki.engine.block.real.RealBlockManager;
import com.ki.engine.config.ConfigManager;
import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.enchantment.EnchantmentManager;
import com.ki.engine.enchantment.KiEnchantment;
import com.ki.engine.entity.EntityManager;
import com.ki.engine.entity.KiMob;
import com.ki.engine.gui.GuiManager;
import com.ki.engine.item.ItemManager;
import com.ki.engine.item.KiItem;
import com.ki.engine.npc.NpcManager;
import com.ki.engine.particle.ParticleManager;
import com.ki.engine.protocol.ProtocolManager;
import com.ki.engine.recipe.KiRecipe;
import com.ki.engine.recipe.RecipeManager;
import com.ki.engine.rpg.RPGManager;
import com.ki.engine.skill.KiSkill;
import com.ki.engine.skill.SkillManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * KiEngine 对外 API 入口。
 * 附属插件唯一允许直接 import 的类。
 */
public final class KiEngineAPI {
    private static KiEnginePlugin plugin;
    private static AddonRegistry addonRegistry;

    public static void init(KiEnginePlugin p) {
        plugin = p;
        addonRegistry = new AddonRegistry(p);
    }

    public static boolean isReady() {
        return plugin != null;
    }

    public static ItemManager getItemManager() { return plugin.getItemManager(); }
    public static BlockManager getBlockManager() { return plugin.getBlockManager(); }
    public static EntityManager getEntityManager() { return plugin.getEntityManager(); }
    public static RecipeManager getRecipeManager() { return plugin.getRecipeManager(); }
    public static SkillManager getSkillManager() { return plugin.getSkillManager(); }
    public static NpcManager getNpcManager() { return plugin.getNpcManager(); }
    public static RPGManager getRpgManager() { return plugin.getRpgManager(); }
    public static GuiManager getGuiManager() { return plugin.getGuiManager(); }
    public static EnchantmentManager getEnchantmentManager() { return plugin.getEnchantmentManager(); }
    public static ConfigManager getConfigManager() { return plugin.getConfigManager(); }
    public static AddonManager getAddonManager() { return plugin.getAddonManager(); }

    public static AddonRegistry getAddonRegistry() { return addonRegistry; }
    public static KiEnginePlugin getPlugin() { return plugin; }

    public static void registerEnchantmentEffect(com.ki.engine.enchantment.effect.EnchantmentEffect effect) {
        if (addonRegistry != null) addonRegistry.registerEnchantmentEffect(effect);
    }

    public static void executeReloadHooks() {
        if (addonRegistry != null) addonRegistry.executeReloadHooks();
    }

    // ===== 快捷注册方法（供 Addon 使用）=====

    public static void registerItem(String addonId, KiItem item) {
        if (addonRegistry != null) addonRegistry.registerItem(addonId, item);
    }
    public static void registerBlock(String addonId, KiBlock block) {
        if (addonRegistry != null) addonRegistry.registerBlock(addonId, block);
    }
    public static void registerMob(String addonId, KiMob mob) {
        if (addonRegistry != null) addonRegistry.registerMob(addonId, mob);
    }
    public static void registerRecipe(String addonId, KiRecipe recipe) {
        if (addonRegistry != null) addonRegistry.registerRecipe(addonId, recipe);
    }
    public static void registerSkill(String addonId, KiSkill skill) {
        if (addonRegistry != null) addonRegistry.registerSkill(addonId, skill);
    }
    public static void registerEnchantment(String addonId, KiEnchantment enchantment) {
        if (addonRegistry != null) addonRegistry.registerEnchantment(addonId, enchantment);
    }
    public static void registerListener(String addonId, Listener listener) {
        if (addonRegistry != null) addonRegistry.registerListener(addonId, listener);
    }
    public static File getAddonDataFolder(String addonId) {
        return addonRegistry != null ? addonRegistry.getAddonDataFolder(addonId) : null;
    }
    public static YamlConfiguration getAddonConfig(String addonId) {
        return addonRegistry != null ? addonRegistry.getAddonConfig(addonId) : new YamlConfiguration();
    }
    public static Connection getDatabaseConnection() throws SQLException {
        if (addonRegistry == null) throw new SQLException("AddonRegistry not initialized");
        return addonRegistry.getDatabaseConnection();
    }

    public static ProtocolManager getProtocolManager() {
        return plugin != null ? plugin.getProtocolManager() : null;
    }
    public static RealBlockManager getRealBlockManager() {
        return plugin != null ? plugin.getRealBlockManager() : null;
    }
    public static ParticleManager getParticleManager() {
        return plugin != null ? plugin.getParticleManager() : null;
    }
}
