package com.ki.engine.enchantment.effect;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 粒子特效 - 在目标或施法者位置生成粒子
 * 参数: type(粒子类型), count(数量), offset(偏移), speed(速度)
 */
public class ParticleEffect implements EnchantmentEffect {

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                        Event triggerEvent, Map<String, String> params) {
        String typeName = params.get("type");
        if (typeName == null) return;

        try {
            Particle particle = Particle.valueOf(typeName.toUpperCase(java.util.Locale.ROOT));
            LivingEntity spawnAt = Boolean.parseBoolean(params.getOrDefault("at_caster", "false")) ? caster : target;
            if (spawnAt == null) spawnAt = caster;

            Location loc = spawnAt.getLocation().add(0, 1, 0);
            int count = parseInt(params.get("count"), 20);
            double offset = parseDouble(params.get("offset"), 0.5);
            double speed = parseDouble(params.get("speed"), 0.1);

            loc.getWorld().spawnParticle(particle, loc, count, offset, offset, offset, speed);
        } catch (IllegalArgumentException e) {
            // Unknown particle type, silently ignore
        }
    }

    @Override
    public String getTypeId() { return "PARTICLE"; }

    private int parseInt(String s, int def) {
        try { return s != null ? Integer.parseInt(s) : def; } catch (Exception e) { return def; }
    }

    private double parseDouble(String s, double def) {
        try { return s != null ? Double.parseDouble(s) : def; } catch (Exception e) { return def; }
    }
}
