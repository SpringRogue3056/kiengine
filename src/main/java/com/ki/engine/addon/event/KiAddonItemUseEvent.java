package com.ki.engine.addon.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Addon 物品使用事件 - 玩家右键使用 KiEngine 物品时触发
 * 附属插件可监听此事件实现自定义交互逻辑
 */
public class KiAddonItemUseEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String itemId;
    private final ItemStack item;
    private final String ability;
    private boolean cancelled = false;

    public KiAddonItemUseEvent(Player player, String itemId, ItemStack item, String ability) {
        this.player = player;
        this.itemId = itemId;
        this.item = item;
        this.ability = ability;
    }

    public Player getPlayer() { return player; }
    public String getItemId() { return itemId; }
    public ItemStack getItem() { return item; }
    public String getAbility() { return ability; }
    public boolean hasAbility() { return ability != null && !ability.isEmpty(); }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
