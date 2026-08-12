package com.ki.engine.survival;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * RPG等级属性加成监听器
 */
public class RPGAttributeListener implements Listener {

    private final KiEnginePlugin plugin;

    public RPGAttributeListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyAttributes(event.getPlayer());
    }

    public void applyAttributes(Player player) {
        int combatLevel = plugin.getRpgManager().getLevel(player, "combat");
        int miningLevel = plugin.getRpgManager().getLevel(player, "mining");

        double bonusHealth = combatLevel * 0.5;
        double bonusDamage = combatLevel * 0.2;
        double bonusSpeed = miningLevel * 0.01;
        double bonusArmor = miningLevel * 0.1;

        applyModifier(player, Attribute.GENERIC_MAX_HEALTH, bonusHealth, "ki_health");
        applyModifier(player, Attribute.GENERIC_ATTACK_DAMAGE, bonusDamage, "ki_damage");
        applyModifier(player, Attribute.GENERIC_MOVEMENT_SPEED, bonusSpeed, "ki_speed");
        applyModifier(player, Attribute.GENERIC_ARMOR, bonusArmor, "ki_armor");

        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null && player.getHealth() > attr.getValue()) {
            player.setHealth(attr.getValue());
        }
    }

    private void applyModifier(Player player, Attribute attribute, double value, String name) {
        var attr = player.getAttribute(attribute);
        if (attr == null) return;

        attr.getModifiers().stream()
            .filter(m -> m.getName().startsWith("ki_"))
            .forEach(attr::removeModifier);

        if (value > 0) {
            attr.addModifier(new AttributeModifier(
                UUID.nameUUIDFromBytes(name.getBytes()),
                name,
                value,
                AttributeModifier.Operation.ADD_NUMBER
            ));
        }
    }
}
