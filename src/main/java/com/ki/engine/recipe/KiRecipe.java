package com.ki.engine.recipe;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * KiEngine 配方定义
 * 支持多种配方类型：shaped/shapeless/smelting/cutting/cooking
 */
public class KiRecipe {
    public enum Type {
        SHAPED, SHAPELESS, SMELTING, CUTTING, COOKING
    }

    private final String id;
    private final Type type;
    private final String resultId;
    private final int resultAmount;
    private final List<String> ingredients;   // 原料ID列表
    private final String toolId;              // 所需工具（切割用）
    private final String containerId;         // 所需容器（烹饪用）
    private final int cookTime;               // 烹饪时间（tick）
    private final double experience;          // 经验奖励

    public KiRecipe(String id, Type type, String resultId, int resultAmount,
                    List<String> ingredients, String toolId, String containerId,
                    int cookTime, double experience) {
        this.id = id;
        this.type = type;
        this.resultId = resultId;
        this.resultAmount = resultAmount;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        this.toolId = toolId;
        this.containerId = containerId;
        this.cookTime = cookTime;
        this.experience = experience;
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public String getResultId() { return resultId; }
    public int getResultAmount() { return resultAmount; }
    public List<String> getIngredients() { return ingredients; }
    public String getToolId() { return toolId; }
    public String getContainerId() { return containerId; }
    public int getCookTime() { return cookTime; }
    public double getExperience() { return experience; }
}
