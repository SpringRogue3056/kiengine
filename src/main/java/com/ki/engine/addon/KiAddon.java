package com.ki.engine.addon;

import com.ki.engine.core.KiEnginePlugin;

/**
 * KiEngine 附属插件接口。
 * 任何 jar 包中的类实现此接口并标注 @KiAddonMeta，即可被 AddonManager 自动加载。
 *
 * 生命周期：
 *   onLoad()    -> KiEngine 完成 PRE_INIT 后调用（此时 ConfigManager 已就绪）
 *   onEnable()  -> KiEngine 完成 INIT 后调用（此时所有 Manager 已就绪）
 *   onDisable() -> KiEngine 关闭时调用
 */
public interface KiAddon {

    /**
     * 加载阶段。可在此读取附属自己的配置文件、注册自定义效果。
     * @param plugin KiEngine 主插件实例
     */
    void onLoad(KiEnginePlugin plugin);

    /**
     * 启用阶段。可在此注册监听器、命令、任务，访问所有 Manager。
     * @param plugin KiEngine 主插件实例
     */
    void onEnable(KiEnginePlugin plugin);

    /**
     * 关闭阶段。清理资源、保存数据。
     * @param plugin KiEngine 主插件实例
     */
    void onDisable(KiEnginePlugin plugin);

    /**
     * 重载阶段。/ki reload 时触发。
     * @param plugin KiEngine 主插件实例
     */
    default void onReload(KiEnginePlugin plugin) {
        onDisable(plugin);
        onLoad(plugin);
        onEnable(plugin);
    }
}
