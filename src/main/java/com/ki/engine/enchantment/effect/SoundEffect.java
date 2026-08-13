package com.ki.engine.enchantment.effect;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 音效效果 - 播放声音
 * 参数: type(声音类型), volume(音量), pitch(音高)
 */
public class SoundEffect implements EnchantmentEffect {

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                        Event triggerEvent, Map<String, String> params) {
        String typeName = params.get("type");
        if (typeName == null) return;

        try {
            Sound sound = Sound.valueOf(typeName.toUpperCase(java.util.Locale.ROOT));
            LivingEntity playAt = Boolean.parseBoolean(params.getOrDefault("at_caster", "true")) ? caster : target;
            if (playAt == null) return;

            Location loc = playAt.getLocation();
            float volume = (float) parseDouble(params.get("volume"), 1.0);
            float pitch = (float) parseDouble(params.get("pitch"), 1.0);

            loc.getWorld().playSound(loc, sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Unknown sound type, silently ignore
        }
    }

    @Override
    public String getTypeId() { return "SOUND"; }

    private double parseDouble(String s, double def) {
        try { return s != null ? Double.parseDouble(s) : def; } catch (Exception e) { return def; }
    }
}
