package com.ki.engine.farmersdelight;

import com.ki.engine.core.KiEnginePlugin;

/**
 * 农夫乐事管理器 - 整合烹饪/切割/作物/钓鱼/炉灶/煎锅
 */
public class FarmersDelightManager {
    private final KiEnginePlugin plugin;

    public FarmersDelightManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.getLogger().info("[FarmersDelight] 重载配置");
    }
}
