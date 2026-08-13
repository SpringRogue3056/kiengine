package com.ki.engine.gui.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SimpleGui 全局监听器 - 支持 SimpleGui / PagedGui / AnimatedGui / InputGui
 */
public class SimpleGuiListener implements Listener {

    private static final Map<UUID, SimpleGui> activeGuis = new ConcurrentHashMap<>();
    private static final Map<UUID, PagedGui> activePagedGuis = new ConcurrentHashMap<>();
    private static final Map<UUID, AnimatedGui> activeAnimatedGuis = new ConcurrentHashMap<>();
    private static final Map<UUID, InputGui> activeChatInputs = new ConcurrentHashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        // PagedGui
        PagedGui paged = activePagedGuis.get(uuid);
        if (paged != null && event.getInventory().equals(paged.getInventory())) {
            event.setCancelled(true);
            paged.handleClick(event.getRawSlot());
            return;
        }

        // AnimatedGui
        AnimatedGui animated = activeAnimatedGuis.get(uuid);
        if (animated != null && event.getInventory().equals(animated.getInventory())) {
            event.setCancelled(true);
            animated.handleClick(event.getRawSlot());
            return;
        }

        // SimpleGui
        SimpleGui gui = activeGuis.get(uuid);
        if (gui != null && event.getInventory().equals(gui.getInventory())) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < gui.getSize()) {
                gui.handleClick(slot);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (activeGuis.containsKey(uuid) || activePagedGuis.containsKey(uuid) || activeAnimatedGuis.containsKey(uuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        PagedGui paged = activePagedGuis.remove(uuid);
        if (paged != null) {
            paged.handleClose();
            return;
        }

        AnimatedGui animated = activeAnimatedGuis.remove(uuid);
        if (animated != null) {
            animated.handleClose();
            return;
        }

        SimpleGui gui = activeGuis.remove(uuid);
        if (gui != null) {
            gui.handleClose();
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        InputGui input = activeChatInputs.get(player.getUniqueId());
        if (input != null) {
            event.setCancelled(true);
            // Run sync to handle the input
            org.bukkit.Bukkit.getScheduler().runTask(
                org.bukkit.Bukkit.getPluginManager().getPlugin("KiEngine"),
                () -> input.handleChatInput(event.getMessage())
            );
        }
    }

    // ========== 注册/注销方法 ==========

    public static void registerGui(UUID uuid, SimpleGui gui) {
        activeGuis.put(uuid, gui);
    }

    public static void unregisterGui(UUID uuid) {
        activeGuis.remove(uuid);
    }

    public static void registerPagedGui(UUID uuid, PagedGui gui) {
        activePagedGuis.put(uuid, gui);
    }

    public static void unregisterPagedGui(UUID uuid) {
        activePagedGuis.remove(uuid);
    }

    public static void registerAnimatedGui(UUID uuid, AnimatedGui gui) {
        activeAnimatedGuis.put(uuid, gui);
    }

    public static void unregisterAnimatedGui(UUID uuid) {
        activeAnimatedGuis.remove(uuid);
    }

    public static void registerChatInput(UUID uuid, InputGui gui) {
        activeChatInputs.put(uuid, gui);
    }

    public static void unregisterChatInput(UUID uuid) {
        activeChatInputs.remove(uuid);
    }
}
