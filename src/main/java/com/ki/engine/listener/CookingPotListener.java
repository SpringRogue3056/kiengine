package com.ki.engine.listener;

import com.ki.engine.block.BlockManagerImpl;
import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.gui.GuiManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 烹饪锅多方块结构监听器
 * 篝火（点燃）+ 上面坩埚 = 烹饪锅
 */
public class CookingPotListener implements Listener {

    private final KiEnginePlugin plugin;

    public CookingPotListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        // 右键点击坩埚
        if (block.getType() != Material.CAULDRON) return;

        // 检查下方是否是点燃的篝火
        Block below = block.getLocation().subtract(0, 1, 0).getBlock();
        if (!isCampfire(below.getType())) return;
        if (!isLit(below)) {
            event.getPlayer().sendMessage("\u00a7c\u3010\u70f9\u996a\u9505\u3011\u718a\u706b\u5df2\u718f\u706d\uff0c\u8bf7\u5148\u70b9\u71c3\uff01");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        plugin.getGuiManager().openCookingPot(player, block.getLocation());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CAULDRON) {
            Block below = block.getLocation().subtract(0, 1, 0).getBlock();
            if (isCampfire(below.getType())) {
                plugin.getGuiManager().onPotDestroyed(block.getLocation());
            }
        }
        if (isCampfire(block.getType())) {
            Block above = block.getLocation().add(0, 1, 0).getBlock();
            if (above.getType() == Material.CAULDRON) {
                plugin.getGuiManager().onPotDestroyed(above.getLocation());
            }
        }
    }

    private boolean isCampfire(Material material) {
        return material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE;
    }

    private boolean isLit(Block block) {
        if (!isCampfire(block.getType())) return false;
        if (block.getBlockData() instanceof org.bukkit.block.data.Lightable) {
            return ((org.bukkit.block.data.Lightable) block.getBlockData()).isLit();
        }
        return false;
    }
}
