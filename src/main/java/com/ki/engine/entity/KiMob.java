package com.ki.engine.entity;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.attribute.Attribute;

import java.util.ArrayList;
import java.util.List;

/**
 * KiEngine 自定义实体定义
 * 融合 Pandora 的怪物属性/技能 + SentienceEntity 的 NPC 交互
 */
public class KiMob {
    private final String id;
    private final EntityType baseType;
    private final String displayName;
    private final double maxHealth;
    private final double damage;
    private final double speed;
    private final double armor;
    private final List<String> skills;       // 技能ID列表
    private final List<String> drops;        // 掉落物ID列表
    private final boolean isNpc;             // 是否为NPC（不攻击玩家）
    private final String interactAction;     // NPC交互动作

    public KiMob(String id, EntityType baseType, String displayName,
                 double maxHealth, double damage, double speed, double armor,
                 List<String> skills, List<String> drops,
                 boolean isNpc, String interactAction) {
        this.id = id;
        this.baseType = baseType;
        this.displayName = displayName;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.speed = speed;
        this.armor = armor;
        this.skills = skills != null ? skills : new ArrayList<>();
        this.drops = drops != null ? drops : new ArrayList<>();
        this.isNpc = isNpc;
        this.interactAction = interactAction;
    }

    public void apply(LivingEntity entity) {
        entity.setCustomName(displayName);
        entity.setCustomNameVisible(true);
        if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
            entity.setHealth(maxHealth);
        }
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        }
        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }
        if (entity.getAttribute(Attribute.GENERIC_ARMOR) != null) {
            entity.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
        }
        if (isNpc) {
            entity.setAI(false);
            entity.setCollidable(false);
        }
    }

    public String getId() { return id; }
    public EntityType getBaseType() { return baseType; }
    public String getDisplayName() { return displayName; }
    public double getMaxHealth() { return maxHealth; }
    public double getDamage() { return damage; }
    public double getSpeed() { return speed; }
    public double getArmor() { return armor; }
    public boolean isNpc() { return isNpc; }
    public String getInteractAction() { return interactAction; }
    public List<String> getSkills() { return skills; }
    public List<String> getDrops() { return drops; }
}
