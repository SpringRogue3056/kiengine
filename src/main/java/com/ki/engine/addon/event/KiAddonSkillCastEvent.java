package com.ki.engine.addon.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Addon 技能施放事件 - KiEngine 技能施放时触发
 */
public class KiAddonSkillCastEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final String skillId;
    private final String skillName;
    private final LivingEntity caster;
    private final LivingEntity target;
    private boolean cancelled = false;

    public KiAddonSkillCastEvent(String skillId, String skillName, LivingEntity caster, LivingEntity target) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.caster = caster;
        this.target = target;
    }

    public String getSkillId() { return skillId; }
    public String getSkillName() { return skillName; }
    public LivingEntity getCaster() { return caster; }
    public LivingEntity getTarget() { return target; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
