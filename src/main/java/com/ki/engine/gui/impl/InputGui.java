package com.ki.engine.gui.impl;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;

/**
 * 输入 GUI - 使用铁砧或告示牌获取玩家输入
 */
public class InputGui implements Listener {

    private final KiEnginePlugin plugin;
    private final Player player;
    private final String title;
    private final String defaultText;
    private Consumer<String> submitHandler;
    private Runnable cancelHandler;
    private Inventory anvilInventory;

    public InputGui(KiEnginePlugin plugin, Player player, String title, String defaultText) {
        this.plugin = plugin;
        this.player = player;
        this.title = title;
        this.defaultText = defaultText;
    }

    public static InputGui create(KiEnginePlugin plugin, Player player, String title, String defaultText) {
        return new InputGui(plugin, player, title, defaultText);
    }

    public InputGui onSubmit(Consumer<String> handler) {
        this.submitHandler = handler;
        return this;
    }

    public InputGui onCancel(Runnable handler) {
        this.cancelHandler = handler;
        return this;
    }

    /**
     * 打开铁砧输入界面
     */
    public void openAnvil() {
        // Create a fake anvil using a standard inventory (since AnvilInventory is server-side)
        anvilInventory = Bukkit.createInventory(null, 9, title);
        
        // Input slot
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(defaultText);
            paper.setItemMeta(meta);
        }
        anvilInventory.setItem(0, paper);
        
        // Confirm button
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§a确认");
            confirm.setItemMeta(confirmMeta);
        }
        anvilInventory.setItem(4, confirm);
        
        // Cancel button
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§c取消");
            cancel.setItemMeta(cancelMeta);
        }
        anvilInventory.setItem(8, cancel);
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(anvilInventory);
    }

    /**
     * 使用聊天栏输入（更简单可靠）
     */
    public void openChat() {
        player.closeInventory();
        player.sendMessage("§a请输入内容（输入 'cancel' 取消）：");
        // Store in a map for the listener to pick up
        SimpleGuiListener.registerChatInput(player.getUniqueId(), this);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != anvilInventory) return;
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot == 4) {
            // Confirm
            ItemStack input = anvilInventory.getItem(0);
            String text = input != null && input.hasItemMeta() && input.getItemMeta().hasDisplayName() 
                ? input.getItemMeta().getDisplayName() : defaultText;
            if (submitHandler != null) submitHandler.accept(text);
            close();
        } else if (slot == 8) {
            // Cancel
            if (cancelHandler != null) cancelHandler.run();
            close();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != anvilInventory) return;
        close();
    }

    private void close() {
        HandlerList.unregisterAll(this);
        if (anvilInventory != null) player.closeInventory();
    }

    public void handleChatInput(String message) {
        if (message.equalsIgnoreCase("cancel")) {
            if (cancelHandler != null) cancelHandler.run();
        } else {
            if (submitHandler != null) submitHandler.accept(message);
        }
        SimpleGuiListener.unregisterChatInput(player.getUniqueId());
    }
}
