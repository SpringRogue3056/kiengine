package com.ki.engine.skill;

import com.ki.engine.registry.Registry;
import org.bukkit.entity.LivingEntity;

/**
 * 技能管理器 - 融合 Pandora 技能系统
 * 条件 -> 目标选择 -> 效果执行
 */
public interface SkillManager {
    Registry<KiSkill> getRegistry();
    void castSkill(String skillId, LivingEntity caster, LivingEntity target);
    void reload();
}
