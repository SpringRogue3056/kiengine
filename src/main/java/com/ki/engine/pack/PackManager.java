package com.ki.engine.pack;

/**
 * 资源包管理器 - 融合 CraftEngine 的 PackManager
 * 自动生成资源包、下发客户端、防破解
 */
public interface PackManager {
    void generatePack();
    void applyToPlayer(org.bukkit.entity.Player player);
    void reload();
}
