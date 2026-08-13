package com.ki.engine.gui.kamenu;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * KiEngine - KaMenu 深度桥接层
 *
 * 将 KaMenu 的 Dialog/Container GUI 能力无缝集成到 KiEngine 中。
 * 提供统一的菜单打开接口，同时向 KaMenu 注册 ki: 命名空间动作处理器，
 * 使 KaMenu 的 YAML 配置可以直接调用 KiEngine 的所有功能。
 *
 * 架构关系：
 *   KiEngine (内容引擎)  <--API-->  KaMenu (GUI引擎)
 *        |                               |
 *        +---- KiMenuBridge --------------+
 *        |        (统一接口层)             |
 *        +---- KiMenuActionHandler --------> (ki:give, ki:mob, ki:skill...)
 *        +---- KiMenuGenerator <---------- (动态生成菜单YAML)
 */
public class KiMenuBridge {

    private final KiEnginePlugin plugin;
    private final KiMenuActionHandler actionHandler;
    private final KiMenuGenerator generator;
    private boolean kaMenuAvailable = false;

    public KiMenuBridge(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.actionHandler = new KiMenuActionHandler(plugin);
        this.generator = new KiMenuGenerator(plugin);
        detectKaMenu();
    }

    /**
     * 检测 KaMenu 是否已加载并初始化桥接
     */
    private void detectKaMenu() {
        if (plugin.getServer().getPluginManager().getPlugin("KaMenu") == null) {
            plugin.getLogger().warning("[KiMenu] KaMenu not found. GUI features will be limited to basic inventory.");
            return;
        }
        try {
            // 尝试加载 KaMenuAPI 类
            Class.forName("org.katacr.kamenu.api.KaMenuAPI");
            kaMenuAvailable = true;
            plugin.getLogger().info("[KiMenu] KaMenu detected. Deep integration enabled.");

            // 注册 KiEngine 动作命名空间
            actionHandler.register();

            // 生成 KiEngine 动态菜单到 KaMenu 目录
            generator.generateAll();

        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("[KiMenu] KaMenu found but API class not available.");
        }
    }

    /**
     * 打开 KiEngine 主菜单（由 KaMenu 驱动）
     */
    public void openMainMenu(Player player) {
        if (kaMenuAvailable) {
            openKaMenu(player, "kiengine/main_menu");
        } else {
            // Fallback: 基础 Inventory GUI
            plugin.getGuiManager().openFallbackMainMenu(player);
        }
    }

    /**
     * 打开物品图鉴
     */
    public void openItemCompendium(Player player) {
        if (kaMenuAvailable) {
            openKaMenu(player, "kiengine/item_compendium");
        } else {
            plugin.getGuiManager().openFallbackItemList(player);
        }
    }

    /**
     * 打开配方浏览器
     */
    public void openRecipeBrowser(Player player) {
        if (kaMenuAvailable) {
            openKaMenu(player, "kiengine/recipe_browser");
        }
    }

    /**
     * 打开 RPG 状态面板
     */
    public void openRpgStatus(Player player) {
        if (kaMenuAvailable) {
            openKaMenu(player, "kiengine/rpg_status", player.getName());
        }
    }

    /**
     * 打开烹饪锅 GUI
     */
    public void openCookingPot(Player player, String potId) {
        if (kaMenuAvailable) {
            openKaMenu(player, "kiengine/cooking_pot", potId);
        } else {
            plugin.getGuiManager().openFallbackCookingPot(player);
        }
    }

    /**
     * 打开 NPC 对话
     */
    public void openNpcDialog(Player player, String npcId, String dialogId) {
        if (kaMenuAvailable) {
            openKaMenu(player, "kiengine/npc_dialog", npcId, dialogId);
        }
    }

    /**
     * 打开技能选择面板
     */
    public void openSkillPanel(Player player) {
        if (kaMenuAvailable) {
            openKaMenu(player, "kiengine/skill_panel");
        }
    }

    /**
     * 打开实体图鉴
     */
    public void openMobCompendium(Player player) {
        if (kaMenuAvailable) {
            openKaMenu(player, "kiengine/mob_compendium");
        }
    }

    /**
     * 通过 KaMenuAPI 打开菜单（反射调用，避免编译时依赖）
     */
    private void openKaMenu(Player player, String menuId, String... args) {
        try {
            Class<?> apiClass = Class.forName("org.katacr.kamenu.api.KaMenuAPI");
            java.lang.reflect.Method method = apiClass.getMethod("openMenu",
                Player.class, String.class, java.util.List.class);
            method.invoke(null, player, menuId, java.util.Arrays.asList(args));
        } catch (Exception e) {
            plugin.getLogger().warning("[KiMenu] Failed to open menu " + menuId + ": " + e.getMessage());
            player.sendMessage("\u00a7cMenu system error. Please contact admin.");
        }
    }

    /**
     * 从内存 YAML 动态打开菜单
     */
    public void openDynamicMenu(Player player, YamlConfiguration config, String contextId) {
        if (!kaMenuAvailable) return;
        try {
            Class<?> apiClass = Class.forName("org.katacr.kamenu.api.KaMenuAPI");
            java.lang.reflect.Method method = apiClass.getMethod("openConfig",
                Player.class, YamlConfiguration.class, String.class, java.util.List.class);
            method.invoke(null, player, config, contextId, java.util.Collections.emptyList());
        } catch (Exception e) {
            plugin.getLogger().warning("[KiMenu] Failed to open dynamic menu: " + e.getMessage());
        }
    }

    /**
     * 重新生成所有动态菜单（在内容重载后调用）
     */
    public void regenerateMenus() {
        if (kaMenuAvailable) {
            generator.generateAll();
        }
    }

    /**
     * 注销动作处理器（插件卸载时调用）
     */
    public void unregister() {
        if (kaMenuAvailable) {
            actionHandler.unregister();
        }
    }

    public boolean isKaMenuAvailable() {
        return kaMenuAvailable;
    }

    public KiMenuGenerator getGenerator() {
        return generator;
    }

    /**
     * 直接打开指定 KaMenu（供内部 action handler 使用）
     */
    public void openKaMenuDirect(Player player, String menuId, String... args) {
        if (kaMenuAvailable) {
            openKaMenu(player, menuId, args);
        }
    }
}
