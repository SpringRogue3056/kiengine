package com.ki.engine.enchantment;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import com.ki.engine.enchantment.effect.*;
import com.ki.engine.registry.Registry;
import com.ki.engine.registry.SimpleRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 附魔管理器 - 管理所有自定义附魔的定义、注册、效果解析
 * 支持从 YAML 配置加载，运行时动态注册新附魔效果
 */
public class EnchantmentManager implements Manager {

    private final KiEnginePlugin plugin;
    private final Registry<KiEnchantment> registry = new SimpleRegistry<>();
    /** 效果类型注册表: typeId -> effect instance */
    private final Map<String, EnchantmentEffect> effectRegistry = new ConcurrentHashMap<>();
    /** 物品附魔缓存: itemId -> Map<enchantId, level> (从配置预加载) */
    private final Map<String, Map<String, Integer>> itemEnchantments = new ConcurrentHashMap<>();

    public EnchantmentManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
        registerDefaultEffects();
    }

    @Override
    public void init() {
        reload();
    }

    /**
     * 注册内置附魔效果
     */
    private void registerDefaultEffects() {
        registerEffect(new DamageEffect());
        registerEffect(new HealEffect());
        registerEffect(new PotionEffectApply());
        registerEffect(new ParticleEffect());
        registerEffect(new SoundEffect());
        registerEffect(new LifeStealEffect());
        registerEffect(new IgniteEffect());
        registerEffect(new KnockbackEffect());
        registerEffect(new ExecuteEffect());
        registerEffect(new AreaEffect());
    }

    /**
     * 注册自定义效果（供外部插件扩展）
     */
    public void registerEffect(EnchantmentEffect effect) {
        effectRegistry.put(effect.getTypeId().toUpperCase(java.util.Locale.ROOT), effect);
    }

    public EnchantmentEffect getEffect(String typeId) {
        return effectRegistry.get(typeId.toUpperCase(java.util.Locale.ROOT));
    }

    public Registry<KiEnchantment> getRegistry() {
        return registry;
    }

    /**
     * 获取物品预配置的附魔
     */
    public Map<String, Integer> getItemEnchantments(String itemId) {
        return itemEnchantments.getOrDefault(itemId.toLowerCase(java.util.Locale.ROOT), Collections.emptyMap());
    }

    /**
     * 检查两个附魔是否冲突
     */
    public boolean conflicts(String enchantId1, String enchantId2) {
        if (enchantId1.equalsIgnoreCase(enchantId2)) return true;
        KiEnchantment e1 = registry.get(enchantId1);
        KiEnchantment e2 = registry.get(enchantId2);
        if (e1 == null || e2 == null) return false;
        // 如果任一附魔与原版冲突，简化处理为不互相冲突（由具体实现决定）
        return false;
    }

    @Override
    public void reload() {
        registry.clear();
        itemEnchantments.clear();
        Map<String, YamlConfiguration> configs = plugin.getConfigManager().getConfigsByType("enchantments");
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            loadEnchantments(entry.getValue());
        }
        // 同时加载物品附魔配置
        Map<String, YamlConfiguration> itemConfigs = plugin.getConfigManager().getConfigsByType("items");
        for (Map.Entry<String, YamlConfiguration> entry : itemConfigs.entrySet()) {
            loadItemEnchantments(entry.getValue());
        }
        plugin.getLogger().info("[EnchantmentManager] Loaded " + registry.size() + " enchantments, " + itemEnchantments.size() + " item enchantment sets");
    }

    private void loadEnchantments(YamlConfiguration config) {
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            try {
                String id = key;
                String displayName = section.getString("display_name", id);
                int maxLevel = section.getInt("max_level", 5);
                List<String> lore = section.getStringList("lore");
                List<String> triggerStrings = section.getStringList("triggers");
                List<KiEnchantment.TriggerType> triggers = new ArrayList<>();
                for (String t : triggerStrings) {
                    try { triggers.add(KiEnchantment.TriggerType.valueOf(t.toUpperCase(java.util.Locale.ROOT))); }
                    catch (IllegalArgumentException ignored) {}
                }
                List<String> effects = section.getStringList("effects");
                Map<String, Double> levelScaling = new HashMap<>();
                ConfigurationSection scalingSec = section.getConfigurationSection("level_scaling");
                if (scalingSec != null) {
                    for (String sk : scalingSec.getKeys(false)) {
                        levelScaling.put(sk, scalingSec.getDouble(sk));
                    }
                }
                boolean hidden = section.getBoolean("hidden", false);
                String rarity = section.getString("rarity", "common");
                boolean conflictsWithVanilla = section.getBoolean("conflicts_with_vanilla", false);

                KiEnchantment enchant = new KiEnchantment(id, displayName, maxLevel, lore,
                        triggers, effects, levelScaling, hidden, rarity, conflictsWithVanilla);
                registry.register(id, enchant);
            } catch (Exception e) {
                plugin.getLogger().warning("[EnchantmentManager] Failed to load enchantment: " + key + " - " + e.getMessage());
            }
        }
    }

    private void loadItemEnchantments(YamlConfiguration config) {
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            try {
                ConfigurationSection enchantSec = section.getConfigurationSection("enchantments");
                if (enchantSec != null) {
                    Map<String, Integer> enchants = new HashMap<>();
                    for (String enchantKey : enchantSec.getKeys(false)) {
                        enchants.put(enchantKey, enchantSec.getInt(enchantKey, 1));
                    }
                    itemEnchantments.put(key.toLowerCase(java.util.Locale.ROOT), enchants);
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * 解析效果参数字符串 "KEY:VALUE;KEY2:VALUE2"
     */
    public static Map<String, String> parseEffectParams(String effectString) {
        Map<String, String> params = new HashMap<>();
        if (effectString == null || effectString.isEmpty()) return params;
        String[] pairs = effectString.split(";");
        for (String pair : pairs) {
            int eq = pair.indexOf(':');
            if (eq > 0) {
                params.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        return params;
    }
}
