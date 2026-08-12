package com.ki.engine.enchantment.effect;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;

/**
 * 击退效果 - 将目标击退
 * 参数: power(力度), scale_per_level(每级增加), vertical(垂直力度比例)
 */
public class KnockbackEffect implements EnchantmentEffect {

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                        Event triggerEvent, Map<String, String> params) {
        if (target == null || caster == null) return;

        double base = parseDouble(params.get("power"), 1.0);
        double perLevel = parseDouble(params.get("scale_per_level"), 0.3);
        double power = base + perLevel * (level - 1);
        double vertical = parseDouble(params.get("vertical"), 0.3);

        Vector direction = target.getLocation().toVector()
                .subtract(caster.getLocation().toVector()).normalize();
        direction.setY(vertical);
        target.setVelocity(direction.multiply(power));
    }

    @Override
    public String getTypeId() { return "KNOCKBACK"; }

    private double parseDouble(String s, double def) {
        try { return s != null ? Double.parseDouble(s) : def; } catch (Exception e) { return def; }
    }
}
