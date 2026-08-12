package com.ki.engine.enchantment.effect;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 斩杀效果 - 目标血量低于阈值时直接击杀
 * 参数: threshold_percent(阈值百分比), scale_per_level(每级降低阈值)
 */
public class ExecuteEffect implements EnchantmentEffect {

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                        Event triggerEvent, Map<String, String> params) {
        if (target == null) return;

        double baseThreshold = parseDouble(params.get("threshold_percent"), 20.0);
        double perLevel = parseDouble(params.get("scale_per_level"), 3.0);
        double threshold = baseThreshold + perLevel * (level - 1);

        var attr = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;

        double maxHealth = attr.getValue();
        double currentPercent = target.getHealth() / maxHealth * 100.0;

        if (currentPercent <= threshold) {
            target.setHealth(0);
            // 播放斩杀特效
            target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);
        }
    }

    @Override
    public String getTypeId() { return "EXECUTE"; }

    private double parseDouble(String s, double def) {
        try { return s != null ? Double.parseDouble(s) : def; } catch (Exception e) { return def; }
    }
}
