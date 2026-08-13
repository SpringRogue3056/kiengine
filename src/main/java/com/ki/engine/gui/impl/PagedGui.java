package com.ki.engine.gui.impl;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 翻页 GUI - 支持大量物品分页显示
 */
public class PagedGui {

    private final KiEnginePlugin plugin;
    private final Player player;
    private final String titleTemplate;
    private final int pageSize; // 每页显示的物品槽位（不含导航）
    private final int rows;
    private final List<GuiItem> items = new ArrayList<>();
    private final Map<Integer, GuiItem> navigation = new HashMap<>();
    private int currentPage = 0;
    private Consumer<Player> closeHandler;
    private BukkitTask updateTask;
    private Inventory currentInventory;

    // 导航按钮
    private GuiItem prevButton = GuiItem.of(Material.ARROW).name("§e上一页");
    private GuiItem nextButton = GuiItem.of(Material.ARROW).name("§e下一页");
    private GuiItem infoButton = GuiItem.of(Material.PAPER).name("§7第 %page%/%total% 页");

    public PagedGui(KiEnginePlugin plugin, Player player, String title, int rows) {
        this.plugin = plugin;
        this.player = player;
        this.titleTemplate = title;
        this.rows = Math.min(rows, 6);
        this.pageSize = (rows - 1) * 9; // 最后一行作为导航栏
    }

    public static PagedGui create(KiEnginePlugin plugin, Player player, String title, int rows) {
        return new PagedGui(plugin, player, title, rows);
    }

    public PagedGui addItem(GuiItem item) {
        items.add(item);
        return this;
    }

    public PagedGui addItems(List<GuiItem> items) {
        this.items.addAll(items);
        return this;
    }

    public PagedGui prevButton(GuiItem button) {
        this.prevButton = button;
        return this;
    }

    public PagedGui nextButton(GuiItem button) {
        this.nextButton = button;
        return this;
    }

    public PagedGui onClose(Consumer<Player> handler) {
        this.closeHandler = handler;
        return this;
    }

    public void open() {
        openPage(0);
    }

    public void openPage(int page) {
        this.currentPage = Math.max(0, Math.min(page, getTotalPages() - 1));
        String title = titleTemplate.replace("%page%", String.valueOf(currentPage + 1))
                                     .replace("%total%", String.valueOf(getTotalPages()));
        currentInventory = Bukkit.createInventory(null, rows * 9, title);

        // 填充物品
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, items.size());
        for (int i = 0; i < end - start; i++) {
            GuiItem guiItem = items.get(start + i);
            if (guiItem.isVisible(player, i)) {
                currentInventory.setItem(i, guiItem.getItem(player));
            }
        }

        // 导航栏（最后一行）
        int navRow = (rows - 1) * 9;
        if (currentPage > 0) {
            currentInventory.setItem(navRow + 3, prevButton.getItem());
            navigation.put(navRow + 3, prevButton);
        }

        infoButton.name("§7第 " + (currentPage + 1) + "/" + getTotalPages() + " 页");
        currentInventory.setItem(navRow + 4, infoButton.getItem());

        if (currentPage < getTotalPages() - 1) {
            currentInventory.setItem(navRow + 5, nextButton.getItem());
            navigation.put(navRow + 5, nextButton);
        }

        // 关闭按钮
        GuiItem close = GuiItem.of(Material.BARRIER).name("§c关闭").onClick(p -> p.closeInventory());
        currentInventory.setItem(navRow + 8, close.getItem());
        navigation.put(navRow + 8, close);

        player.openInventory(currentInventory);
        SimpleGuiListener.registerPagedGui(player.getUniqueId(), this);

        // 启动动画更新
        startAnimationTask();
    }

    private void startAnimationTask() {
        if (updateTask != null) updateTask.cancel();
        updateTask = plugin.getScheduler().runTimer(() -> {
            if (currentInventory == null) return;
            boolean updated = false;
            for (int i = 0; i < currentInventory.getSize(); i++) {
                int itemIndex = currentPage * pageSize + i;
                if (itemIndex < items.size()) {
                    GuiItem guiItem = items.get(itemIndex);
                    guiItem.nextFrame();
                    if (guiItem.getUpdateInterval() > 0) {
                        currentInventory.setItem(i, guiItem.getItem(player));
                        updated = true;
                    }
                }
            }
            if (updated) player.updateInventory();
        }, 20, 20);
    }

    public void handleClick(int slot) {
        // 检查导航按钮
        GuiItem nav = navigation.get(slot);
        if (nav != null && nav.getClickHandler() != null) {
            nav.getClickHandler().accept(player);
            return;
        }

        // 检查翻页
        int navRow = (rows - 1) * 9;
        if (slot == navRow + 3 && currentPage > 0) {
            openPage(currentPage - 1);
            return;
        }
        if (slot == navRow + 5 && currentPage < getTotalPages() - 1) {
            openPage(currentPage + 1);
            return;
        }
        if (slot == navRow + 8) {
            player.closeInventory();
            return;
        }

        // 检查物品点击
        int itemIndex = currentPage * pageSize + slot;
        if (itemIndex >= 0 && itemIndex < items.size()) {
            GuiItem guiItem = items.get(itemIndex);
            if (guiItem.getClickHandler() != null) {
                guiItem.getClickHandler().accept(player);
            }
        }
    }

    public void handleClose() {
        if (updateTask != null) updateTask.cancel();
        SimpleGuiListener.unregisterPagedGui(player.getUniqueId());
        if (closeHandler != null) closeHandler.accept(player);
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) items.size() / pageSize));
    }

    public Inventory getInventory() {
        return currentInventory;
    }
}
