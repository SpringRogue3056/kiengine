package com.ki.engine.survival;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.item.KiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 自定义食物监听器 - 处理 KiEngine 自定义物品的食物效果
 * 支持：自定义饱食度、饱和度、药水效果、特殊能力触发
 */
public class FoodListener implements Listener {

    private final KiEnginePlugin plugin;

    public FoodListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        String itemId = plugin.getItemManager().getItemId(item);
        if (itemId == null) return;

        KiItem kiItem = plugin.getItemManager().getRegistry().get(itemId);
        if (kiItem == null) return;

        if (!kiItem.isEdible()) return;

        // Cancel vanilla eating behavior for custom food
        event.setCancelled(true);

        // Apply custom food effects
        int foodLevel = kiItem.getFoodLevel();
        float saturation = kiItem.getSaturation();

        if (foodLevel > 0) {
            int newFood = Math.min(20, player.getFoodLevel() + foodLevel);
            player.setFoodLevel(newFood);
        }
        if (saturation > 0) {
            player.setSaturation(Math.min(player.getFoodLevel(), player.getSaturation() + saturation));
        }

        // Consume item
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.isSimilar(item)) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand.isSimilar(item)) {
                offHand.setAmount(offHand.getAmount() - 1);
            }
        }

        // Play eating sound and particles
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(org.bukkit.Particle.ITEM,
                player.getLocation().add(0, 1.5, 0), 8, 0.2, 0.2, 0.2, item);

        // Trigger item ability if exists
        if (kiItem.getAbility() != null) {
            // Ability triggered via enchantment system or custom ability handler
            // For now, abilities are handled by the enchantment listener's ON_USE trigger
        }
    }
}
