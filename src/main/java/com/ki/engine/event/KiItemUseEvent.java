package com.ki.engine.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * 自定义物品使用事件 - 玩家右键使用 KiEngine 物品时触发
 * 附属插件可监听此事件实现自定义交互逻辑
 */
public class KiItemUseEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String itemId;
    private final ItemStack item;
    private boolean cancelled = false;

    public KiItemUseEvent(Player player, String itemId, ItemStack item) {
        this.player = player;
        this.itemId = itemId;
        this.item = item;
    }

    public Player getPlayer() { return player; }
    public String getItemId() { return itemId; }
    public ItemStack getItem() { return item; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
