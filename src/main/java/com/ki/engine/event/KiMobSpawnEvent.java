package com.ki.engine.event;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class KiMobSpawnEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final String mobId;
    private final LivingEntity entity;
    private final Location location;
    private boolean cancelled = false;

    public KiMobSpawnEvent(String mobId, LivingEntity entity, Location location) {
        this.mobId = mobId;
        this.entity = entity;
        this.location = location;
    }

    public String getMobId() { return mobId; }
    public LivingEntity getEntity() { return entity; }
    public Location getLocation() { return location; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
