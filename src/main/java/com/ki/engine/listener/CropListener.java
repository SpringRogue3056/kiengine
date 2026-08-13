package com.ki.engine.listener;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.crop.CropManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 作物监听器 - 自定义作物种植/生长/收获
 * 使用PDC标记自定义作物，支持配置驱动
 */
public class CropListener implements Listener {

    private final KiEnginePlugin plugin;
    private final NamespacedKey cropKey;

    public CropListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.cropKey = new NamespacedKey(plugin, "ki_crop");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        if (item == null) return;

        String itemId = plugin.getItemManager().getItemId(item);
        if (itemId == null) return;

        CropManager.CropData crop = plugin.getCropManager().getCrop(itemId);
        if (crop == null) return;

        // 标记为自定义作物
        Block block = event.getBlockPlaced();
        block.setType(crop.baseBlock());
        var pdc = new org.bukkit.persistence.PersistentDataContainer[] { null };
        // 方块没有PDC，使用文件存储位置映射
        plugin.getCropManager().setCropLocation(block.getLocation(), crop.seedId());
        player.sendMessage("\u00a7e你种下了 " + crop.seedId() + " 种子");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String cropId = plugin.getCropManager().getCropAt(block.getLocation());
        if (cropId == null) return;

        CropManager.CropData crop = plugin.getCropManager().getCrop(cropId);
        if (crop == null) return;

        event.setDropItems(false);

        // 检查是否成熟
        boolean isMature = true;
        if (block.getBlockData() instanceof Ageable ageable) {
            isMature = ageable.getAge() == ageable.getMaximumAge();
        }

        if (isMature) {
            // 掉落成熟产物
            ItemStack drop = plugin.getItemManager().getItem(crop.matureDrop());
            if (drop != null) {
                int amount = crop.minDrop() + ThreadLocalRandom.current().nextInt(crop.maxDrop() - crop.minDrop() + 1);
                drop.setAmount(amount);
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }

        // 概率返还种子
        if (ThreadLocalRandom.current().nextDouble() < crop.seedReturnChance()) {
            ItemStack seeds = plugin.getItemManager().getItem(crop.seedDrop());
            if (seeds != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), seeds);
            }
        }

        plugin.getCropManager().removeCropLocation(block.getLocation());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        ItemStack hand = event.getItem();

        // 骨粉催熟自定义作物
        if (hand != null && hand.getType() == Material.BONE_MEAL) {
            String cropId = plugin.getCropManager().getCropAt(block.getLocation());
            if (cropId == null) return;

            if (block.getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() < ageable.getMaximumAge()) {
                    ageable.setAge(Math.min(ageable.getAge() + 2, ageable.getMaximumAge()));
                    block.setBlockData(ageable);
                    if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                        hand.setAmount(hand.getAmount() - 1);
                    }
                    player.playSound(block.getLocation(), org.bukkit.Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);
                    event.setCancelled(true);
                }
            }
        }
    }
}
