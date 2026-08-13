package com.ki.engine.addon.event;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;

/**
 * Addon 方块交互事件 - 玩家与 KiEngine 自定义方块交互时触发
 */
public class KiAddonBlockInteractEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String blockId;
    private final Block block;
    private final Location location;
    private final Action action;
    private boolean cancelled = false;

    public KiAddonBlockInteractEvent(Player player, String blockId, Block block, Action action) {
        this.player = player;
        this.blockId = blockId;
        this.block = block;
        this.location = block.getLocation();
        this.action = action;
    }

    public Player getPlayer() { return player; }
    public String getBlockId() { return blockId; }
    public Block getBlock() { return block; }
    public Location getLocation() { return location; }
    public Action getAction() { return action; }
    public boolean isRightClick() { return action == Action.RIGHT_CLICK_BLOCK; }
    public boolean isLeftClick() { return action == Action.LEFT_CLICK_BLOCK; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
