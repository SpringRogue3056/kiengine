package com.ki.engine.enchantment.effect;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 范围效果 - 对周围实体造成伤害或效果
 * 参数: radius(半径), effect_type(子效果类型), amount, 继承子效果参数
 */
public class AreaEffect implements EnchantmentEffect {

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                        Event triggerEvent, Map<String, String> params) {
        if (caster == null) return;

        double baseRadius = parseDouble(params.get("radius"), 3.0);
        double perLevel = parseDouble(params.get("radius_per_level"), 0.5);
        double radius = baseRadius + perLevel * (level - 1);

        String subEffectType = params.get("effect_type");
        if (subEffectType == null) return;

        Location center = target != null ? target.getLocation() : caster.getLocation();

        for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity le && entity != caster) {
                // 根据子效果类型执行不同逻辑
                switch (subEffectType.toUpperCase(java.util.Locale.ROOT)) {
                    case "DAMAGE" -> {
                        double dmg = parseDouble(params.get("amount"), 3.0) + parseDouble(params.get("scale_per_level"), 1.0) * (level - 1);
                        le.damage(dmg, caster);
                    }
                    case "POTION" -> {
                        String potionType = params.get("potion_type");
                        if (potionType != null) {
                            var pet = org.bukkit.potion.PotionEffectType.getByName(potionType.toUpperCase(java.util.Locale.ROOT));
                            if (pet != null) {
                                int duration = (int) (parseDouble(params.get("duration"), 3.0) * 20);
                                int amplifier = parseInt(params.get("amplifier"), 0);
                                le.addPotionEffect(new org.bukkit.potion.PotionEffect(pet, duration, amplifier));
                            }
                        }
                    }
                    case "KNOCKBACK" -> {
                        double power = parseDouble(params.get("power"), 1.0);
                        org.bukkit.util.Vector dir = le.getLocation().toVector().subtract(center.toVector()).normalize();
                        dir.setY(0.3);
                        le.setVelocity(dir.multiply(power));
                    }
                }
            }
        }

        // 范围粒子
        center.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, center.add(0, 0.5, 0), (int) radius * 5, radius / 2, 0.5, radius / 2);
    }

    @Override
    public String getTypeId() { return "AREA"; }

    @Override
    public boolean supportsNullTarget() { return true; }

    private double parseDouble(String s, double def) {
        try { return s != null ? Double.parseDouble(s) : def; } catch (Exception e) { return def; }
    }

    private int parseInt(String s, int def) {
        try { return s != null ? Integer.parseInt(s) : def; } catch (Exception e) { return def; }
    }
}
