package com.ki.engine.enchantment.effect;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 治疗效果 - 恢复生命值
 * 参数: amount(基础治疗量), scale_per_level(每级增加), target_self(是否对自己)
 */
public class HealEffect implements EnchantmentEffect {

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                        Event triggerEvent, Map<String, String> params) {
        boolean targetSelf = Boolean.parseBoolean(params.getOrDefault("target_self", "true"));
        LivingEntity healTarget = targetSelf ? caster : target;
        if (healTarget == null) return;

        double base = parseDouble(params.get("amount"), 2.0);
        double perLevel = parseDouble(params.get("scale_per_level"), 1.0);
        double amount = base + perLevel * (level - 1);

        var attr = healTarget.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            double max = attr.getValue();
            healTarget.setHealth(Math.min(healTarget.getHealth() + amount, max));
        }
    }

    @Override
    public String getTypeId() { return "HEAL"; }

    private double parseDouble(String s, double def) {
        try { return s != null ? Double.parseDouble(s) : def; } catch (Exception e) { return def; }
    }
}
