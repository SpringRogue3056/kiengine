package com.ki.engine.enchantment.effect;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * 药水效果施加 - 给目标或自己添加药水效果
 * 参数: type(药水类型), duration(持续秒数), amplifier(等级), target_self
 */
public class PotionEffectApply implements EnchantmentEffect {

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                        Event triggerEvent, Map<String, String> params) {
        boolean targetSelf = Boolean.parseBoolean(params.getOrDefault("target_self", "false"));
        LivingEntity effectTarget = targetSelf ? caster : target;
        if (effectTarget == null) return;

        String typeName = params.get("type");
        if (typeName == null) return;

        PotionEffectType pet = PotionEffectType.getByName(typeName.toUpperCase(java.util.Locale.ROOT));
        if (pet == null) return;

        double baseDuration = parseDouble(params.get("duration"), 3.0);
        double perLevel = parseDouble(params.get("duration_per_level"), 0.0);
        int durationTicks = (int) ((baseDuration + perLevel * (level - 1)) * 20);
        int baseAmp = parseInt(params.get("amplifier"), 0);
        int ampPerLevel = parseInt(params.get("amplifier_per_level"), 0);
        int amplifier = baseAmp + ampPerLevel * (level - 1);

        effectTarget.addPotionEffect(new PotionEffect(pet, Math.max(durationTicks, 20), amplifier));
    }

    @Override
    public String getTypeId() { return "POTION"; }

    private double parseDouble(String s, double def) {
        try { return s != null ? Double.parseDouble(s) : def; } catch (Exception e) { return def; }
    }

    private int parseInt(String s, int def) {
        try { return s != null ? Integer.parseInt(s) : def; } catch (Exception e) { return def; }
    }
}
