package com.ki.engine.skill;

import java.util.ArrayList;
import java.util.List;

/**
 * KiEngine 技能定义
 * 融合 Pandora 的条件-目标-效果链
 */
public class KiSkill {
    private final String id;
    private final String displayName;
    private final double cooldown;        // 冷却时间（秒）
    private final double manaCost;        // 魔力消耗
    private final List<String> conditions;   // 触发条件
    private final List<String> targeters;      // 目标选择器
    private final List<String> mechanics;      // 效果执行器

    public KiSkill(String id, String displayName, double cooldown, double manaCost,
                   List<String> conditions, List<String> targeters, List<String> mechanics) {
        this.id = id;
        this.displayName = displayName;
        this.cooldown = cooldown;
        this.manaCost = manaCost;
        this.conditions = conditions != null ? conditions : new ArrayList<>();
        this.targeters = targeters != null ? targeters : new ArrayList<>();
        this.mechanics = mechanics != null ? mechanics : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public double getCooldown() { return cooldown; }
    public double getManaCost() { return manaCost; }
    public List<String> getConditions() { return conditions; }
    public List<String> getTargeters() { return targeters; }
    public List<String> getMechanics() { return mechanics; }
}
