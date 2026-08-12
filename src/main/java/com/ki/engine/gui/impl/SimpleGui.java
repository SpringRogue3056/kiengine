package com.ki.engine.gui.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 内置简易GUI系统 - 不依赖KaMenu
 * 支持：分页、点击回调、动态更新
 */
public class SimpleGui implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();
    private final String guiId;

    public SimpleGui(String title, int rows, String guiId) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        this.guiId = guiId;
    }

    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        if (handler != null) clickHandlers.put(slot, handler);
    }

    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    public void fill(ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
            }
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public boolean handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> handler = clickHandlers.get(event.getSlot());
        if (handler != null) {
            handler.accept(event);
            return true;
        }
        return false;
    }

    public String getGuiId() { return guiId; }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
