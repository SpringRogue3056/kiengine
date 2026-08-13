package com.ki.engine.listener;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GuiClickListener implements Listener {
    private final KiEnginePlugin plugin;

    public GuiClickListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("\u00a78KiEngine")) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        if (title.contains("Main Menu")) {
            switch (event.getSlot()) {
                case 11 -> plugin.getGuiManager().openFallbackItemList(player);
                case 13 -> player.sendMessage("\u00a7eRecipe browser requires KaMenu. Install KaMenu for full GUI.");
                case 15 -> player.sendMessage("\u00a7eRPG Status: Combat " + plugin.getRpgManager().getLevel(player, "combat") + " | Mining " + plugin.getRpgManager().getLevel(player, "mining"));
            }
        } else if (title.contains("Item Compendium")) {
            // 物品图鉴点击显示详情
        } else if (title.contains("Cooking Pot")) {
            // 烹饪锅点击逻辑
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 清理临时数据
    }
}
