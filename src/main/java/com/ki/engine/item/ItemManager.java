package com.ki.engine.item;

import com.ki.engine.registry.Registry;
import org.bukkit.inventory.ItemStack;

/**
 * 物品管理器 - 融合 CraftEngine 自定义物品系统
 * 支持自定义材质、NBT、食物属性、耐久
 */
public interface ItemManager {
    Registry<KiItem> getRegistry();
    ItemStack getItem(String id);
    ItemStack getItem(String id, int amount);
    String getItemId(ItemStack item);
    boolean isCustomItem(ItemStack item);
    void reload();
}
