package com.ki.engine.listener;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BlockInteractListener implements Listener {
    private final KiEnginePlugin plugin;

    public BlockInteractListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        String itemId = plugin.getItemManager().getItemId(event.getItemInHand());
        if (itemId == null) return;
        Location loc = event.getBlock().getLocation();
        plugin.getBlockManager().placeBlock(itemId, loc);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        String blockId = plugin.getBlockManager().getBlockId(loc);
        if (blockId == null) return;
        event.setDropItems(false);
        org.bukkit.inventory.ItemStack drop = plugin.getItemManager().getItem(blockId);
        if (drop != null) loc.getWorld().dropItemNaturally(loc, drop);
        plugin.getBlockManager().removeBlock(loc);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Location loc = block.getLocation();
        String blockId = plugin.getBlockManager().getBlockId(loc);
        if (blockId == null) return;
        if (plugin.getBlockManager() instanceof com.ki.engine.block.BlockManagerImpl impl) {
            if (impl.isCookingPot(loc)) event.getPlayer().sendMessage("§6你打开了烹饪锅...");
            else if (impl.isCuttingBoard(loc)) event.getPlayer().sendMessage("§6你使用了砧板...");
        }
    }
}
