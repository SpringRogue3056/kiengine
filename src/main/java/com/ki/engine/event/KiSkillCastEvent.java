package com.ki.engine.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class KiSkillCastEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final String skillId;
    private final LivingEntity caster;
    private final LivingEntity target;
    private boolean cancelled = false;

    public KiSkillCastEvent(String skillId, LivingEntity caster, LivingEntity target) {
        this.skillId = skillId;
        this.caster = caster;
        this.target = target;
    }

    public String getSkillId() { return skillId; }
    public LivingEntity getCaster() { return caster; }
    public LivingEntity getTarget() { return target; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
