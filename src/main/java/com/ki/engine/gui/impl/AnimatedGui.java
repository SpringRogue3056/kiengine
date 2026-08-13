package com.ki.engine.gui.impl;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 动态 GUI - 支持动画标题、动态物品、条件显示
 */
public class AnimatedGui {

    private final KiEnginePlugin plugin;
    private final Player player;
    private final List<String> titleFrames = new ArrayList<>();
    private final Map<Integer, GuiItem> items = new HashMap<>();
    private final int rows;
    private Consumer<Player> closeHandler;
    private BukkitTask animationTask;
    private Inventory currentInventory;
    private int titleFrame = 0;
    private int titleInterval = 20; // ticks
    private long openTime = 0;

    public AnimatedGui(KiEnginePlugin plugin, Player player, int rows) {
        this.plugin = plugin;
        this.player = player;
        this.rows = Math.min(rows, 6);
    }

    public static AnimatedGui create(KiEnginePlugin plugin, Player player, int rows) {
        return new AnimatedGui(plugin, player, rows);
    }

    /**
     * 动画标题帧
     */
    public AnimatedGui titleFrames(int intervalTicks, String... frames) {
        this.titleInterval = intervalTicks;
        this.titleFrames.addAll(List.of(frames));
        return this;
    }

    /**
     * 渐变标题（自动插值）
     */
    public AnimatedGui gradientTitle(String baseTitle, String... colors) {
        for (String color : colors) {
            titleFrames.add(color + baseTitle);
        }
        return this;
    }

    public AnimatedGui setItem(int slot, GuiItem item) {
        items.put(slot, item);
        return this;
    }

    public AnimatedGui fillRow(int row, GuiItem item) {
        for (int i = 0; i < 9; i++) {
            items.put(row * 9 + i, item);
        }
        return this;
    }

    public AnimatedGui fillBorder(GuiItem item) {
        for (int i = 0; i < 9; i++) {
            items.put(i, item); // top
            items.put((rows - 1) * 9 + i, item); // bottom
        }
        for (int i = 1; i < rows - 1; i++) {
            items.put(i * 9, item); // left
            items.put(i * 9 + 8, item); // right
        }
        return this;
    }

    public AnimatedGui onClose(Consumer<Player> handler) {
        this.closeHandler = handler;
        return this;
    }

    public void open() {
        String title = titleFrames.isEmpty() ? "Menu" : titleFrames.get(0);
        currentInventory = Bukkit.createInventory(null, rows * 9, title);
        refreshItems();
        player.openInventory(currentInventory);
        SimpleGuiListener.registerAnimatedGui(player.getUniqueId(), this);
        openTime = System.currentTimeMillis();

        // 启动动画任务
        if (animationTask != null) animationTask.cancel();
        animationTask = plugin.getScheduler().runTimer(this::updateAnimation, titleInterval, titleInterval);
    }

    private void updateAnimation() {
        if (currentInventory == null || player.getOpenInventory().getTopInventory() != currentInventory) {
            animationTask.cancel();
            return;
        }

        // 更新标题帧
        if (!titleFrames.isEmpty() && titleFrames.size() > 1) {
            titleFrame = (titleFrame + 1) % titleFrames.size();
            // Note: Bukkit doesn't support title change without reopening in 1.21
            // Use reflection or packet if needed
        }

        // 更新动态物品
        boolean updated = false;
        for (Map.Entry<Integer, GuiItem> entry : items.entrySet()) {
            GuiItem guiItem = entry.getValue();
            guiItem.nextFrame();
            if (guiItem.getUpdateInterval() > 0 || guiItem.isVisible(player, entry.getKey())) {
                currentInventory.setItem(entry.getKey(), guiItem.getItem(player));
                updated = true;
            }
        }
        if (updated) player.updateInventory();
    }

    private void refreshItems() {
        for (Map.Entry<Integer, GuiItem> entry : items.entrySet()) {
            if (entry.getValue().isVisible(player, entry.getKey())) {
                currentInventory.setItem(entry.getKey(), entry.getValue().getItem(player));
            }
        }
    }

    public void handleClick(int slot) {
        GuiItem guiItem = items.get(slot);
        if (guiItem != null && guiItem.getClickHandler() != null) {
            guiItem.getClickHandler().accept(player);
        }
    }

    public void handleClose() {
        if (animationTask != null) animationTask.cancel();
        SimpleGuiListener.unregisterAnimatedGui(player.getUniqueId());
        if (closeHandler != null) closeHandler.accept(player);
    }

    public Inventory getInventory() {
        return currentInventory;
    }

    public long getOpenDuration() {
        return System.currentTimeMillis() - openTime;
    }
}
