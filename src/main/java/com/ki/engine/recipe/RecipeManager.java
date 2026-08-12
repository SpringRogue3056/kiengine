package com.ki.engine.recipe;

import com.ki.engine.registry.Registry;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 配方管理器 - 融合 CraftEngine 配方 + Pandora 技能条件系统
 * 支持工作台、熔炉、砧板、烹饪锅配方
 */
public interface RecipeManager {
    Registry<KiRecipe> getRegistry();
    KiRecipe findMatchingRecipe(String type, List<ItemStack> ingredients, ItemStack tool);
    void reload();
}
