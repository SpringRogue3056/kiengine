package com.ki.engine.enchantment.effect;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 生命偷取效果 - 根据造成的伤害恢复生命
 * 参数: percent(偷取百分比), scale_per_level(每级增加百分比)
 */
public class LifeStealEffect implements EnchantmentEffect {

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                        Event triggerEvent, Map<String, String> params) {
        if (caster == null) return;
        if (!(triggerEvent instanceof EntityDamageByEntityEvent e)) return;

        double basePercent = parseDouble(params.get("percent"), 5.0);
        double perLevel = parseDouble(params.get("scale_per_level"), 2.5);
        double percent = basePercent + perLevel * (level - 1);

        double damage = e.getDamage();
        double heal = damage * percent / 100.0;

        var attr = caster.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            caster.setHealth(Math.min(caster.getHealth() + heal, attr.getValue()));
        }
    }

    @Override
    public String getTypeId() { return "LIFE_STEAL"; }

    private double parseDouble(String s, double def) {
        try { return s != null ? Double.parseDouble(s) : def; } catch (Exception e) { return def; }
    }
}
