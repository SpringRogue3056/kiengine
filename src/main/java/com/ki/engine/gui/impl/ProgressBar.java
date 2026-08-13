package com.ki.engine.gui.impl;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI 进度条构建器 - 支持水平/垂直进度条
 */
public class ProgressBar {

    private final int length;
    private final char direction; // 'H' horizontal, 'V' vertical
    private final Material filledMaterial;
    private final Material emptyMaterial;
    private String filledName = "§a";
    private String emptyName = "§7";
    private double progress = 0.0; // 0.0 - 1.0

    public ProgressBar(int length, char direction, Material filled, Material empty) {
        this.length = length;
        this.direction = direction;
        this.filledMaterial = filled;
        this.emptyMaterial = empty;
    }

    public static ProgressBar horizontal(int length, Material filled, Material empty) {
        return new ProgressBar(length, 'H', filled, empty);
    }

    public static ProgressBar vertical(int length, Material filled, Material empty) {
        return new ProgressBar(length, 'V', filled, empty);
    }

    public ProgressBar names(String filled, String empty) {
        this.filledName = filled;
        this.emptyName = empty;
        return this;
    }

    public ProgressBar progress(double value) {
        this.progress = Math.max(0.0, Math.min(1.0, value));
        return this;
    }

    public ProgressBar progress(int current, int max) {
        this.progress = max > 0 ? (double) current / max : 0.0;
        return this;
    }

    /**
     * 生成进度条物品数组
     */
    public List<ItemStack> build() {
        List<ItemStack> result = new ArrayList<>();
        int filledCount = (int) Math.round(progress * length);
        
        for (int i = 0; i < length; i++) {
            boolean isFilled = i < filledCount;
            Material mat = isFilled ? filledMaterial : emptyMaterial;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(isFilled ? filledName : emptyName);
                meta.setLore(List.of("§7" + (int)(progress * 100) + "%"));
                item.setItemMeta(meta);
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 获取单个槽位的物品
     */
    public ItemStack getItem(int index) {
        List<ItemStack> items = build();
        return index >= 0 && index < items.size() ? items.get(index) : new ItemStack(Material.AIR);
    }

    public int getLength() {
        return length;
    }

    public double getProgress() {
        return progress;
    }
}
