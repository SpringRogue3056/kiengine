package com.ki.engine.survival;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.entity.KiMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Boss自动技能监听器 - 自定义实体攻击玩家时概率触发技能
 */
public class MobSkillListener implements Listener {

    private final KiEnginePlugin plugin;

    public MobSkillListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        if (!(event.getEntity() instanceof Player)) return;

        String mobId = getMobId(attacker);
        if (mobId == null) return;

        KiMob mob = plugin.getEntityManager().getMobRegistry().get(mobId);
        if (mob == null) return;

        List<String> skills = mob.getSkills();
        if (skills == null || skills.isEmpty()) return;

        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            String skillId = skills.get(0);
            plugin.getSkillManager().castSkill(skillId, attacker, (LivingEntity) event.getEntity());
        }
    }

    private String getMobId(LivingEntity entity) {
        var pdc = entity.getPersistentDataContainer();
        var key = new org.bukkit.NamespacedKey(plugin, "ki_mob_id");
        return pdc.get(key, PersistentDataType.STRING);
    }
}
