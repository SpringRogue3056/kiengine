package com.ki.engine.gui;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.gui.kamenu.KiMenuBridge;
import com.ki.engine.item.KiItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI 管理器 - 统一所有界面
 *
 * 优先使用 KaMenu 驱动（如果已安装），否则回退到基础 Inventory GUI。
 * 通过 KiMenuBridge 实现与 KaMenu 的深度集成。
 */
public class GuiManager {

    private final KiEnginePlugin plugin;
    private KiMenuBridge menuBridge;

    public GuiManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.menuBridge = new KiMenuBridge(plugin);
    }

    // ==================== 统一入口（自动选择 KaMenu 或回退） ====================

    public void openMainMenu(Player player) {
        menuBridge.openMainMenu(player);
    }

    public void openItemCompendium(Player player) {
        menuBridge.openItemCompendium(player);
    }

    public void openRecipeBrowser(Player player) {
        menuBridge.openRecipeBrowser(player);
    }

    public void openRpgStatus(Player player) {
        menuBridge.openRpgStatus(player);
    }

    public void openCookingPot(Player player, Location potLocation) {
        menuBridge.openCookingPot(player, potLocation.toString());
    }

    public void openNpcDialog(Player player, String npcId, String dialogId) {
        menuBridge.openNpcDialog(player, npcId, dialogId);
    }

    public void openSkillPanel(Player player) {
        menuBridge.openSkillPanel(player);
    }

    public void openMobCompendium(Player player) {
        menuBridge.openMobCompendium(player);
    }

    // Alias methods for KiCommand / KiMenuActionHandler compatibility
    public void openItemBrowser(Player player) {
        openItemCompendium(player);
    }

    public void openRecipeBook(Player player) {
        openRecipeBrowser(player);
    }

    // ==================== 回退 GUI（KaMenu 不可用时） ====================

    public void openFallbackMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "\u00a78KiEngine \u00a77- Main Menu");
        inv.setItem(11, createGuiItem(Material.CHEST, "\u00a7eItem Compendium",
            "\u00a77Browse all custom items",
            "\u00a77Total: \u00a7f" + plugin.getItemManager().getRegistry().keys().size()));
        inv.setItem(13, createGuiItem(Material.CRAFTING_TABLE, "\u00a7eRecipe Browser",
            "\u00a77View all recipes",
            "\u00a77Total: \u00a7f" + plugin.getRecipeManager().getRegistry().keys().size()));
        inv.setItem(15, createGuiItem(Material.EXPERIENCE_BOTTLE, "\u00a7eRPG Status",
            "\u00a77View your levels and stats"));
        player.openInventory(inv);
    }

    public void openFallbackItemList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "\u00a78Item Compendium");
        int slot = 0;
        for (KiItem item : plugin.getItemManager().getRegistry().values()) {
            if (slot >= 54) break;
            ItemStack stack = item.build(plugin);
            inv.setItem(slot++, stack);
        }
        player.openInventory(inv);
    }

    public void openFallbackCookingPot(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "\u00a78Cooking Pot");
        inv.setItem(10, createGuiItem(Material.WHITE_STAINED_GLASS_PANE, "\u00a77Ingredient", "\u00a77Place ingredients here"));
        inv.setItem(12, createGuiItem(Material.WHITE_STAINED_GLASS_PANE, "\u00a77Ingredient", "\u00a77Place ingredients here"));
        inv.setItem(14, createGuiItem(Material.WHITE_STAINED_GLASS_PANE, "\u00a77Ingredient", "\u00a77Place ingredients here"));
        inv.setItem(16, createGuiItem(Material.CAMPFIRE, "\u00a76Fire", "\u00a77Cooking in progress..."));
        inv.setItem(22, createGuiItem(Material.BOWL, "\u00a7eOutput", "\u00a77Result will appear here"));
        player.openInventory(inv);
    }

    // ==================== 工具方法 ====================

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ==================== 桥接访问 ====================

    public KiMenuBridge getMenuBridge() {
        return menuBridge;
    }

    public void onPotDestroyed(Location location) {
        // 通知正在使用此烹饪锅的玩家
        menuBridge.regenerateMenus();
    }
}
