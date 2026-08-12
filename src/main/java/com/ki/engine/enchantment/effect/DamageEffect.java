package com.ki.engine.enchantment.effect;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 额外伤害效果 - 在原有伤害基础上增加固定值或百分比伤害
 * 参数: amount(基础伤害), scale_per_level(每级增加), percent(是否百分比)
 */
public class DamageEffect implements EnchantmentEffect {

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                        Event triggerEvent, Map<String, String> params) {
        if (target == null) return;
        
        double base = parseDouble(params.get("amount"), 2.0);
        double perLevel = parseDouble(params.get("scale_per_level"), 1.0);
        boolean percent = Boolean.parseBoolean(params.getOrDefault("percent", "false"));
        double damage = base + perLevel * (level - 1);

        if (percent && triggerEvent instanceof EntityDamageByEntityEvent e) {
            double original = e.getDamage();
            e.setDamage(original + original * damage / 100.0);
        } else {
            target.damage(damage, caster);
        }
    }

    @Override
    public String getTypeId() { return "DAMAGE"; }

    private double parseDouble(String s, double def) {
        try { return s != null ? Double.parseDouble(s) : def; } catch (Exception e) { return def; }
    }
}
