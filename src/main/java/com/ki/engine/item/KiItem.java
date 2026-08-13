package com.ki.engine.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KiEngine custom item definition with lazy PDC key resolution.
 */
public class KiItem {
    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final int customModelData;
    private final boolean edible;
    private final int foodLevel;
    private final float saturation;
    private final int maxDurability;
    private final boolean unbreakable;
    private final Map<String, Integer> enchantments; // 预置附魔: enchantId -> level
    private final String ability; // 物品能力ID (右键触发)
    private final List<String> flags; // 物品标签 (如 "no_drop", "soulbound")
    private volatile NamespacedKey cachedKey;

    public KiItem(String id, Material material, String displayName, List<String> lore,
                  int customModelData, boolean edible, int foodLevel, float saturation,
                  int maxDurability, boolean unbreakable,
                  Map<String, Integer> enchantments, String ability, List<String> flags) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.customModelData = customModelData;
        this.edible = edible;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.maxDurability = maxDurability;
        this.unbreakable = unbreakable;
        this.enchantments = enchantments != null ? enchantments : Map.of();
        this.ability = ability;
        this.flags = flags != null ? flags : List.of();
    }

    public ItemStack build(Plugin plugin) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            List<String> fullLore = new ArrayList<>(lore);
            fullLore.add("\u00a78ID: " + id);
            meta.setLore(fullLore);
            if (customModelData > 0) meta.setCustomModelData(customModelData);
            if (unbreakable) meta.setUnbreakable(true);

            // Lazy-init cached key
            if (cachedKey == null) {
                cachedKey = new NamespacedKey(plugin, "ki_item_id");
            }
            meta.getPersistentDataContainer().set(cachedKey, PersistentDataType.STRING, id);

            // Apply pre-configured enchantments to PDC
            if (!enchantments.isEmpty()) {
                NamespacedKey enchantKey = new NamespacedKey(plugin, "ki_enchants");
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Integer> e : enchantments.entrySet()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(e.getKey()).append(":").append(e.getValue());
                }
                meta.getPersistentDataContainer().set(enchantKey, PersistentDataType.STRING, sb.toString());
            }

            // Apply ability flag
            if (ability != null && !ability.isEmpty()) {
                NamespacedKey abilityKey = new NamespacedKey(plugin, "ki_ability");
                meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, ability);
            }

            // Apply flags
            if (!flags.isEmpty()) {
                NamespacedKey flagsKey = new NamespacedKey(plugin, "ki_flags");
                meta.getPersistentDataContainer().set(flagsKey, PersistentDataType.STRING, String.join(",", flags));
            }

            item.setItemMeta(meta);
        }
        if (maxDurability > 0 && material.getMaxDurability() > 0) {
            item.setDurability((short) 0);
        }
        return item;
    }

    public ItemStack build(int amount, Plugin plugin) {
        ItemStack item = build(plugin);
        item.setAmount(amount);
        return item;
    }

    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public boolean isEdible() { return edible; }
    public int getFoodLevel() { return foodLevel; }
    public float getSaturation() { return saturation; }
    public int getCustomModelData() { return customModelData; }
    public int getMaxDurability() { return maxDurability; }
    public boolean isUnbreakable() { return unbreakable; }
    public Map<String, Integer> getEnchantments() { return enchantments; }
    public String getAbility() { return ability; }
    public List<String> getFlags() { return flags; }
    public boolean hasFlag(String flag) { return flags.contains(flag); }
}
