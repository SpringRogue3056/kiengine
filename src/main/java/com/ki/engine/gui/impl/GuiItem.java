package com.ki.engine.gui.impl;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * GUI 物品构建器 - 支持条件显示、动态更新、玩家头颅
 */
public class GuiItem {

    private ItemStack item;
    private Consumer<Player> clickHandler;
    private BiPredicate<Player, Integer> condition; // player, slot -> visible
    private Function<Player, ItemStack> dynamicItem;
    private String permission;
    private int updateInterval = 0; // tick, 0 = no animation
    private int animationFrame = 0;
    private List<ItemStack> animationFrames = new ArrayList<>();

    public GuiItem(Material material) {
        this.item = new ItemStack(material);
    }

    public GuiItem(ItemStack item) {
        this.item = item.clone();
    }

    public static GuiItem of(Material material) {
        return new GuiItem(material);
    }

    public static GuiItem of(ItemStack item) {
        return new GuiItem(item);
    }

    public GuiItem name(String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return this;
    }

    public GuiItem lore(List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return this;
    }

    public GuiItem lore(String... lines) {
        return lore(List.of(lines));
    }

    public GuiItem amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public GuiItem customModelData(int cmd) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(cmd);
            item.setItemMeta(meta);
        }
        return this;
    }

    /**
     * 使用玩家头颅
     */
    public GuiItem skull(Player player) {
        if (item.getType() == Material.PLAYER_HEAD) {
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(player);
                item.setItemMeta(meta);
            }
        }
        return this;
    }

    /**
     * 使用指定玩家名的头颅
     */
    public GuiItem skull(String playerName) {
        if (item.getType() == Material.PLAYER_HEAD) {
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
            if (meta != null) {
                meta.setOwner(playerName);
                item.setItemMeta(meta);
            }
        }
        return this;
    }

    public GuiItem onClick(Consumer<Player> handler) {
        this.clickHandler = handler;
        return this;
    }

    /**
     * 条件显示 - 只有满足条件时才显示
     */
    public GuiItem condition(BiPredicate<Player, Integer> condition) {
        this.condition = condition;
        return this;
    }

    public GuiItem condition(String permission) {
        this.permission = permission;
        this.condition = (player, slot) -> player.hasPermission(permission);
        return this;
    }

    /**
     * 动态物品 - 每次打开/刷新时重新生成
     */
    public GuiItem dynamic(Function<Player, ItemStack> supplier) {
        this.dynamicItem = supplier;
        return this;
    }

    /**
     * 动画帧 - 物品会循环切换
     */
    public GuiItem animation(int intervalTicks, ItemStack... frames) {
        this.updateInterval = intervalTicks;
        this.animationFrames = List.of(frames);
        return this;
    }

    public ItemStack getItem(Player player) {
        if (dynamicItem != null) {
            ItemStack dynamic = dynamicItem.apply(player);
            return dynamic != null ? dynamic : item;
        }
        if (!animationFrames.isEmpty() && animationFrames.size() > 0) {
            return animationFrames.get(animationFrame % animationFrames.size());
        }
        return item;
    }

    public ItemStack getItem() {
        return item;
    }

    public boolean isVisible(Player player, int slot) {
        if (condition == null) return true;
        return condition.test(player, slot);
    }

    public Consumer<Player> getClickHandler() {
        return clickHandler;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void nextFrame() {
        if (!animationFrames.isEmpty()) {
            animationFrame = (animationFrame + 1) % animationFrames.size();
        }
    }
}
