package com.ki.engine.npc;

import com.ki.engine.entity.KiMob;
import org.bukkit.Location;

import java.util.UUID;

/**
 * NPC管理器 - 基于 SentienceEntity 的 NPC 系统
 * 支持全息图、路径巡逻、交互对话
 */
public interface NpcManager {
    UUID spawnNpc(String mobId, Location loc, String hologramText);
    void setNpcPath(UUID npcId, java.util.List<Location> path);
    void removeNpc(UUID npcId);
    void reload();
}
