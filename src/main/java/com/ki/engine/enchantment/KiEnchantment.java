package com.ki.engine.enchantment;

import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

/**
 * KiEngine 自定义附魔定义
 * 支持：触发式附魔（攻击/受击/挖掘/使用）、属性加成、粒子特效、音效
 */
public class KiEnchantment {

    private final String id;
    private final String displayName;
    private final int maxLevel;
    private final List<String> lore;
    private final List<TriggerType> triggers;
    private final List<String> effects; // 格式: "效果类型:参数"
    private final Map<String, Double> levelScaling; // 每级属性缩放
    private final boolean hidden; // 是否隐藏附魔（不显示在Lore中）
    private final String rarity; // common, uncommon, rare, epic, legendary
    private final boolean conflictsWithVanilla; // 是否与原版附魔冲突

    public KiEnchantment(String id, String displayName, int maxLevel, List<String> lore,
                         List<TriggerType> triggers, List<String> effects,
                         Map<String, Double> levelScaling, boolean hidden,
                         String rarity, boolean conflictsWithVanilla) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.lore = lore != null ? lore : List.of();
        this.triggers = triggers != null ? triggers : List.of();
        this.effects = effects != null ? effects : List.of();
        this.levelScaling = levelScaling != null ? levelScaling : Map.of();
        this.hidden = hidden;
        this.rarity = rarity;
        this.conflictsWithVanilla = conflictsWithVanilla;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getMaxLevel() { return maxLevel; }
    public List<String> getLore() { return lore; }
    public List<TriggerType> getTriggers() { return triggers; }
    public List<String> getEffects() { return effects; }
    public Map<String, Double> getLevelScaling() { return levelScaling; }
    public boolean isHidden() { return hidden; }
    public String getRarity() { return rarity; }
    public boolean conflictsWithVanilla() { return conflictsWithVanilla; }

    /**
     * 计算某等级下的缩放值
     */
    public double getScaledValue(String key, int level) {
        double base = levelScaling.getOrDefault(key + "_base", 0.0);
        double perLevel = levelScaling.getOrDefault(key + "_per_level", 0.0);
        return base + perLevel * (level - 1);
    }

    /**
     * 获取带等级的显示名称
     */
    public String getDisplayName(int level) {
        if (maxLevel <= 1) return displayName;
        return displayName + " " + toRoman(level);
    }

    /**
     * 获取稀有度颜色代码
     */
    public String getRarityColor() {
        return switch (rarity.toLowerCase()) {
            case "common" -> "\u00a77";
            case "uncommon" -> "\u00a7a";
            case "rare" -> "\u00a79";
            case "epic" -> "\u00a75";
            case "legendary" -> "\u00a76";
            default -> "\u00a7f";
        };
    }

    private static String toRoman(int num) {
        if (num < 1 || num > 10) return String.valueOf(num);
        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return romans[num - 1];
    }

    public enum TriggerType {
        ON_HIT,           // 攻击实体时
        ON_HIT_BY,        // 被攻击时
        ON_KILL,          // 击杀实体时
        ON_MINE,          // 挖掘方块时
        ON_USE,           // 右键使用物品时
        ON_EQUIP,         // 装备时（护甲）
        ON_UNEQUIP,       // 卸下时（护甲）
        ON_HELD,          // 手持时（持续效果）
        ON_PROJECTILE_HIT,// 弹射物命中时
        ON_BLOCK_PLACE,   // 放置方块时
        ON_FISH,          // 钓鱼时
        ON_JUMP,          // 跳跃时
        ON_SNEAK,         // 潜行时
        ON_SPRINT,        // 疾跑时
        PERIODIC          // 周期性触发（手持时每隔X秒）
    }
}
