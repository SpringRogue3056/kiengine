package com.ki.engine.recipe;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import com.ki.engine.item.ItemManager;
import com.ki.engine.registry.Registry;
import com.ki.engine.registry.SimpleRegistry;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Optimized recipe manager with type-indexed lookup and O(n) matching.
 */
public class RecipeManagerImpl implements RecipeManager, Manager {

    private final KiEnginePlugin plugin;
    private final Registry<KiRecipe> registry = new SimpleRegistry<>();
    /** Type-indexed recipes for O(1) filtering */
    private final Map<KiRecipe.Type, List<KiRecipe>> recipesByType = new EnumMap<>(KiRecipe.Type.class);
    private ItemManager itemManager;

    public RecipeManagerImpl(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    /** Dependency injection after construction */
    public void setItemManager(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @Override
    public Registry<KiRecipe> getRegistry() {
        return registry;
    }

    /**
     * O(1) type lookup then O(m) matching where m = recipes of that type.
     */
    @Override
    public KiRecipe findMatchingRecipe(String type, List<ItemStack> ingredients, ItemStack tool) {
        KiRecipe.Type recipeType;
        try {
            recipeType = KiRecipe.Type.valueOf(type.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        List<KiRecipe> candidates = recipesByType.get(recipeType);
        if (candidates == null) return null;
        for (KiRecipe recipe : candidates) {
            if (matches(recipe, ingredients, tool)) return recipe;
        }
        return null;
    }

    /**
     * O(n) matching using frequency counting instead of list removal.
     */
    private boolean matches(KiRecipe recipe, List<ItemStack> ingredients, ItemStack tool) {
        // Tool check
        if (recipe.getToolId() != null && !recipe.getToolId().isEmpty()) {
            if (tool == null) return false;
            String toolId = itemManager != null ? itemManager.getItemId(tool) : null;
            if (toolId == null || !toolId.equalsIgnoreCase(recipe.getToolId())) {
                if (!tool.getType().name().toLowerCase(java.util.Locale.ROOT).contains(
                        recipe.getToolId().toLowerCase(java.util.Locale.ROOT))) {
                    return false;
                }
            }
        }

        // Ingredient frequency counting (O(n))
        Map<String, Integer> required = new HashMap<>();
        for (String ing : recipe.getIngredients()) {
            required.merge(ing.toLowerCase(java.util.Locale.ROOT), 1, Integer::sum);
        }

        int nonEmptyCount = 0;
        for (ItemStack item : ingredients) {
            if (item == null || item.getType() == Material.AIR) continue;
            nonEmptyCount++;
            String itemId = itemManager != null ? itemManager.getItemId(item) : null;
            String matchKey = itemId != null ? itemId.toLowerCase(java.util.Locale.ROOT)
                    : item.getType().name().toLowerCase(java.util.Locale.ROOT);
            Integer count = required.get(matchKey);
            if (count == null) return false;
            if (count == 1) required.remove(matchKey);
            else required.put(matchKey, count - 1);
        }

        // Must match exactly (no extra items, no missing items)
        return required.isEmpty() && nonEmptyCount == recipe.getIngredients().size();
    }

    @Override
    public void reload() {
        registry.clear();
        recipesByType.clear();
        Map<String, YamlConfiguration> configs = plugin.getConfigManager().getConfigsByType("recipes");
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            loadRecipes(entry.getValue());
        }
        // Build type index
        for (KiRecipe recipe : registry.values()) {
            recipesByType.computeIfAbsent(recipe.getType(), k -> new ArrayList<>()).add(recipe);
        }
        plugin.getLogger().info("[RecipeManager] Loaded " + registry.size() + " recipes");
    }

    private void loadRecipes(YamlConfiguration config) {
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            try {
                String id = key;
                KiRecipe.Type type = KiRecipe.Type.valueOf(section.getString("type", "SHAPED").toUpperCase(java.util.Locale.ROOT));
                String resultId = section.getString("result.id", "");
                int resultAmount = section.getInt("result.amount", 1);
                List<String> ingredients = section.getStringList("ingredients");
                String toolId = section.getString("tool", null);
                String containerId = section.getString("container", null);
                int cookTime = section.getInt("cook_time", 200);
                double exp = section.getDouble("experience", 0.0);

                KiRecipe recipe = new KiRecipe(id, type, resultId, resultAmount, ingredients, toolId, containerId, cookTime, exp);
                registry.register(id, recipe);
            } catch (Exception e) {
                plugin.getLogger().warning("[RecipeManager] Failed to load: " + key);
            }
        }
    }
}
