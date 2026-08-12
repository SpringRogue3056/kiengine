package com.ki.engine.enchantment.effect;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 点燃效果 - 让目标着火
 * 参数: duration(持续秒数), scale_per_level(每级增加)
 */
public class IgniteEffect implements EnchantmentEffect {

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                        Event triggerEvent, Map<String, String> params) {
        if (target == null) return;

        double base = parseDouble(params.get("duration"), 2.0);
        double perLevel = parseDouble(params.get("scale_per_level"), 1.0);
        int ticks = (int) ((base + perLevel * (level - 1)) * 20);

        target.setFireTicks(Math.max(ticks, 20));
    }

    @Override
    public String getTypeId() { return "IGNITE"; }

    private double parseDouble(String s, double def) {
        try { return s != null ? Double.parseDouble(s) : def; } catch (Exception e) { return def; }
    }
}
