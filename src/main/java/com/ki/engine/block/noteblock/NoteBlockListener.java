package com.ki.engine.block.noteblock;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listener for NoteBlock-based custom blocks.
 * Handles placement, breaking, interaction, and prevents note sound.
 */
public class NoteBlockListener implements Listener {

    private final KiEnginePlugin plugin;

    public NoteBlockListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || item.getType() != Material.NOTE_BLOCK) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return;

        int cmd = meta.getCustomModelData();
        NoteBlockManager manager = plugin.getNoteBlockManager();
        if (manager == null) return;

        for (NoteBlockData data : manager.getRegistry().values()) {
            if (data.getCustomModelData() == cmd) {
                // Place as custom block
                Location loc = event.getBlock().getLocation();
                manager.placeBlock(data.getId(), loc);
                event.setCancelled(true);
                // Consume item if not creative
                if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        NoteBlockManager manager = plugin.getNoteBlockManager();
        if (manager == null) return;

        NoteBlockData data = manager.getBlockData(block);
        if (data == null) return;

        event.setCancelled(true);
        block.setType(Material.AIR);
        manager.removeBlock(block.getLocation());

        // Drop custom item
        if (data.getDropItem() != null && !data.getDropItem().isEmpty()) {
            ItemStack drop = plugin.getItemManager().getItem(data.getDropItem());
            if (drop != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (block.getType() != Material.NOTE_BLOCK) return;

        NoteBlockManager manager = plugin.getNoteBlockManager();
        if (manager == null) return;

        NoteBlockData data = manager.getBlockData(block);
        if (data == null) return;

        // Prevent note sound
        event.setCancelled(true);

        if (data.isInteractable() && event.getAction().isRightClick()) {
            Player player = event.getPlayer();
            // Call custom interact logic here
            // Could trigger skills, open GUI, etc.
            player.sendMessage("\u00a7aYou interacted with: " + data.getDisplayName());
        }
    }
}
