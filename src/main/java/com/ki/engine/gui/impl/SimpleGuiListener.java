package com.ki.engine.gui.impl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易GUI事件监听器
 */
public class SimpleGuiListener implements Listener {

    private final Map<String, SimpleGui> openGuis = new ConcurrentHashMap<>();

    public void registerGui(SimpleGui gui) {
        // 不需要注册，通过InventoryHolder判断
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SimpleGui gui) {
            event.setCancelled(true);
            gui.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 清理逻辑
    }
}
