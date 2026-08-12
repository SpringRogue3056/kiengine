package com.ki.engine.listener;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 砧板多方块结构监听器
 * 木棍右键橡木原木 → 创建砧板
 * 右键放食材 → 手持刀/斧切割 → 获得切片
 */
public class CuttingBoardListener implements Listener {

    private final KiEnginePlugin plugin;
    // 存储世界中的砧板位置 -> 上面的物品
    private final Map<String, BoardData> boards = new HashMap<>();

    public CuttingBoardListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        ItemStack hand = event.getItem();

        // 情况1：木棍右键橡木原木 → 创建砧板
        if (isOakLog(block.getType())) {
            if (hand != null && hand.getType() == Material.STICK) {
                createBoard(block, player);
                event.setCancelled(true);
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    hand.setAmount(hand.getAmount() - 1);
                }
                return;
            }
        }

        // 情况2：右键已创建的砧板
        String key = getKey(block.getLocation());
        BoardData board = boards.get(key);
        if (board == null) return;

        event.setCancelled(true);

        // 空手 → 取回物品
        if (hand == null || hand.getType() == Material.AIR) {
            if (board.hasItem()) {
                player.getInventory().addItem(board.takeItem());
                player.sendMessage("\u00a77\u53d6\u56de\u4e86 \u00a7f" + board.itemName);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
            } else {
                player.sendMessage("\u00a77\u7827\u677f\u662f\u7a7a\u7684");
            }
            return;
        }

        // 手持工具 → 尝试切割
        String toolType = getToolType(hand);
        if (toolType != null) {
            if (!board.hasItem()) {
                player.sendMessage("\u00a7c\u7827\u677f\u4e0a\u6ca1\u6709\u4e1c\u897f\u53ef\u4ee5\u5207");
                return;
            }
            if (cutItem(board, player, toolType)) {
                player.sendMessage("\u00a7a\u00a7l\u5520\uff01\u00a7r \u00a77\u5207\u5272\u5b8c\u6210");
                player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.8f, 1.2f);
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && hand.getType().getMaxDurability() > 0) {
                    hand.setDurability((short) (hand.getDurability() + 1));
                }
            } else {
                player.sendMessage("\u00a7c\u8fd9\u628a\u5de5\u5177\u5207\u4e0d\u4e86\u8fd9\u4e2a");
            }
            return;
        }

        // 手持食材 → 放到砧板上
        if (!board.hasItem()) {
            board.placeItem(hand.clone());
            player.sendMessage("\u00a77\u5c06 \u00a7f" + hand.getType().name().toLowerCase() + " \u00a77\u653e\u5230\u4e86\u7827\u677f\u4e0a");
            player.playSound(player.getLocation(), Sound.BLOCK_WOOD_PLACE, 0.8f, 1.2f);
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                hand.setAmount(hand.getAmount() - 1);
            }
        } else {
            player.sendMessage("\u00a7c\u7827\u677f\u4e0a\u5df2\u7ecf\u6709\u4e1c\u897f\u4e86\uff0c\u5148\u7a7a\u624b\u53d6\u56de");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String key = getKey(block.getLocation());
        BoardData board = boards.remove(key);
        if (board != null && board.hasItem()) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), board.takeItem());
        }
    }

    private void createBoard(Block block, Player player) {
        String key = getKey(block.getLocation());
        if (boards.containsKey(key)) {
            player.sendMessage("\u00a7c\u8fd9\u91cc\u5df2\u7ecf\u6709\u7827\u677f\u4e86\uff01");
            return;
        }
        boards.put(key, new BoardData());
        player.sendMessage("\u00a7a\u521b\u5efa\u4e86\u7827\u677f\uff01\u53f3\u952e\u653e\u7f6e\u98df\u6750\uff0c\u7528\u5200\u5207\u5272");
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_PLACE, 1.0f, 1.0f);
        block.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                block.getLocation().add(0.5, 1.2, 0.5), 10, 0.3, 0.2, 0.3);
    }

    private boolean cutItem(BoardData board, Player player, String toolType) {
        String itemName = board.itemName;
        ItemStack result = getCutResult(itemName, toolType);
        if (result == null) return false;
        board.clear();
        player.getWorld().dropItemNaturally(player.getLocation(), result);
        return true;
    }

    private ItemStack getCutResult(String itemName, String toolType) {
        // 刀切 → 切片
        if (toolType.equals("knife")) {
            switch (itemName.toUpperCase()) {
                case "CABBAGE": return new ItemStack(Material.KELP, 2); // 占位
                case "TOMATO": return new ItemStack(Material.APPLE, 2); // 占位
                case "ONION": return new ItemStack(Material.POTATO, 2); // 占位
                case "BEEF": return new ItemStack(Material.BEEF, 2);
                case "PORKCHOP": return new ItemStack(Material.PORKCHOP, 2);
                case "CHICKEN": return new ItemStack(Material.CHICKEN, 2);
                case "MELON": return new ItemStack(Material.MELON_SLICE, 4);
                case "PUMPKIN": return new ItemStack(Material.PUMPKIN_PIE, 4); // 占位
            }
        }
        // 斧切 → 劈开
        if (toolType.equals("axe")) {
            switch (itemName.toUpperCase()) {
                case "OAK_LOG": case "BIRCH_LOG": case "SPRUCE_LOG":
                case "JUNGLE_LOG": case "ACACIA_LOG": case "DARK_OAK_LOG":
                    return new ItemStack(Material.STICK, 4);
            }
        }
        return null;
    }

    private String getToolType(ItemStack hand) {
        if (hand == null) return null;
        String typeName = hand.getType().name();
        if (typeName.endsWith("_SWORD")) return "knife";
        if (typeName.endsWith("_AXE")) return "axe";
        return null;
    }

    private boolean isOakLog(Material material) {
        return material == Material.OAK_LOG || material == Material.STRIPPED_OAK_LOG;
    }

    private String getKey(org.bukkit.Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private static class BoardData {
        ItemStack item = null;
        String itemName = "";
        boolean hasItem() { return item != null; }
        void placeItem(ItemStack stack) {
            item = stack;
            itemName = stack.getType().name();
        }
        ItemStack takeItem() {
            ItemStack result = item;
            item = null;
            itemName = "";
            return result;
        }
        void clear() {
            item = null;
            itemName = "";
        }
    }
}
