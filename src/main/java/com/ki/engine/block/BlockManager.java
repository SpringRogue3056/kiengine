package com.ki.engine.block;

import com.ki.engine.registry.Registry;
import org.bukkit.Location;

/**
 * 方块管理器 - 融合 CraftEngine 真实方块注册 + 家具系统
 * 支持自定义方块、家具（碰撞箱+座位）、多方块结构
 */
public interface BlockManager {
    Registry<KiBlock> getRegistry();
    void placeBlock(String id, Location loc);
    String getBlockId(Location loc);
    void removeBlock(Location loc);
    boolean isCustomBlock(Location loc);
    void reload();
}
