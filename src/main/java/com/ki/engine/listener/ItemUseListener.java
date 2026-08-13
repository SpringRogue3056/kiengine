package com.ki.engine.listener;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.event.KiItemUseEvent;
import com.ki.engine.item.KiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ItemUseListener implements Listener {
    private final KiEnginePlugin plugin;

    public ItemUseListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;
        String itemId = plugin.getItemManager().getItemId(item);
        if (itemId == null) return;
        KiItemUseEvent useEvent = new KiItemUseEvent(player, itemId, item);
        plugin.getServer().getPluginManager().callEvent(useEvent);
        if (useEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        KiItem kiItem = plugin.getItemManager().getRegistry().get(itemId);
        if (kiItem != null && kiItem.getAbility() != null) {
            handleAbility(player, item, kiItem.getAbility(), event);
        }
    }

    private void handleAbility(Player player, ItemStack item, String ability, PlayerInteractEvent event) {
        switch (ability.toLowerCase()) {
            case "fireball" -> {
                player.launchProjectile(org.bukkit.entity.SmallFireball.class);
                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GHAST_SHOOT, 1, 1);
            }
            case "heal" -> {
                player.setHealth(Math.min(player.getHealth() + 4, player.getMaxHealth()));
                player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 1, 0), 5);
            }
            case "teleport" -> {
                org.bukkit.util.Vector d = player.getLocation().getDirection().multiply(5);
                player.teleport(player.getLocation().add(d));
                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            default -> plugin.getLogger().info("[ItemUse] Unknown ability: " + ability);
        }
    }
}
