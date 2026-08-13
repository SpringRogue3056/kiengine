package com.ki.engine.item;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import com.ki.engine.registry.Registry;
import com.ki.engine.registry.SimpleRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized item manager with template caching and single-pass PDC lookup.
 */
public class ItemManagerImpl implements ItemManager, Manager {

    private final KiEnginePlugin plugin;
    private final Registry<KiItem> registry = new SimpleRegistry<>();
    private final NamespacedKey itemIdKey;
    /** Template cache: pre-built ItemStack (amount=1) cloned on request */
    private final Map<String, ItemStack> templateCache = new ConcurrentHashMap<>();

    public ItemManagerImpl(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.itemIdKey = new NamespacedKey(plugin, "ki_item_id");
    }

    @Override
    public void init() {
        reload();
    }

    @Override
    public Registry<KiItem> getRegistry() {
        return registry;
    }

    /**
     * Fast clone from template cache instead of rebuilding NBT every time.
     */
    @Override
    public ItemStack getItem(String id) {
        ItemStack template = templateCache.get(id.toLowerCase(java.util.Locale.ROOT));
        if (template == null) return null;
        return template.clone();
    }

    @Override
    public ItemStack getItem(String id, int amount) {
        ItemStack item = getItem(id);
        if (item != null) item.setAmount(amount);
        return item;
    }

    /**
     * Single-pass PDC read: getOrDefault avoids has() + get() double access.
     */
    @Override
    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    @Override
    public boolean isCustomItem(ItemStack item) {
        return getItemId(item) != null;
    }

    @Override
    public void reload() {
        registry.clear();
        templateCache.clear();
        Map<String, YamlConfiguration> configs = plugin.getConfigManager().getConfigsByType("items");
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            loadItems(entry.getValue());
        }
        plugin.getLogger().info("[ItemManager] Loaded " + registry.size() + " items");
    }

    private void loadItems(YamlConfiguration config) {
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            try {
                String id = key;
                Material material = Material.valueOf(section.getString("material", "STONE").toUpperCase(java.util.Locale.ROOT));
                String name = section.getString("name", id);
                List<String> lore = section.getStringList("lore");
                int cmd = section.getInt("custom_model_data", 0);
                boolean edible = section.getBoolean("edible", false);
                int foodLevel = section.getInt("food.level", 0);
                float saturation = (float) section.getDouble("food.saturation", 0.0);
                int maxDurability = section.getInt("durability", 0);
                boolean unbreakable = section.getBoolean("unbreakable", false);

                // Load pre-configured enchantments
                Map<String, Integer> enchantments = new HashMap<>();
                org.bukkit.configuration.ConfigurationSection enchantSec = section.getConfigurationSection("enchantments");
                if (enchantSec != null) {
                    for (String ek : enchantSec.getKeys(false)) {
                        enchantments.put(ek, enchantSec.getInt(ek, 1));
                    }
                }

                String ability = section.getString("ability", null);
                List<String> flags = section.getStringList("flags");

                KiItem kiItem = new KiItem(id, material, name, lore, cmd, edible, foodLevel, saturation, maxDurability, unbreakable, enchantments, ability, flags);
                registry.register(id, kiItem);
                // Pre-build template for fast cloning
                templateCache.put(id.toLowerCase(java.util.Locale.ROOT), kiItem.build(plugin));
            } catch (Exception e) {
                plugin.getLogger().warning("[ItemManager] Failed to load: " + key + " - " + e.getMessage());
            }
        }
    }
}
