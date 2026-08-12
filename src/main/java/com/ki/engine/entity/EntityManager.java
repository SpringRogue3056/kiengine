package com.ki.engine.entity;

import com.ki.engine.registry.Registry;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * 实体管理器 - 融合 Pandora 自定义怪物 + SentienceEntity NPC
 * 统一管理所有自定义实体（怪物/NPC/全息图）
 */
public interface EntityManager {
    Registry<KiMob> getMobRegistry();
    LivingEntity spawnMob(String id, Location loc);
    String getMobId(UUID uuid);
    void removeMob(UUID uuid);
    void reload();
}
