package com.ki.engine.skill;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import com.ki.engine.event.KiSkillCastEvent;
import com.ki.engine.registry.Registry;
import com.ki.engine.registry.SimpleRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill engine with compiled mechanics, condition evaluation, and target selection.
 */
public class SkillManagerImpl implements SkillManager, Manager {

    private final KiEnginePlugin plugin;
    private final Registry<KiSkill> registry = new SimpleRegistry<>();
    /** Compiled mechanic cache: skillId -> list of compiled operations */
    private final Map<String, List<CompiledMechanic>> mechanicCache = new ConcurrentHashMap<>();
    /** Flat cooldown map: "uuid:skillId" -> timestamp */
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public SkillManagerImpl(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        reload();
    }

    @Override public Registry<KiSkill> getRegistry() { return registry; }

    @Override
    public void castSkill(String skillId, LivingEntity caster, LivingEntity target) {
        KiSkill skill = registry.get(skillId);
        if (skill == null) return;

        // Evaluate conditions
        if (!evaluateConditions(skill.getConditions(), caster, target)) return;

        // Resolve target if targeter specified
        LivingEntity resolvedTarget = resolveTarget(skill.getTargeters(), caster, target);

        // Check cooldown
        String cdKey = caster.getUniqueId() + ":" + skillId;
        if (isOnCooldown(cdKey, skill.getCooldown())) {
            if (caster instanceof Player) {
                ((Player) caster).sendMessage("\u00a7cSkill on cooldown...");
            }
            return;
        }

        // Fire event (cancelable)
        KiSkillCastEvent event = new KiSkillCastEvent(skillId, caster, resolvedTarget);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        // Execute compiled mechanics
        List<CompiledMechanic> compiled = mechanicCache.get(skillId);
        if (compiled != null) {
            for (CompiledMechanic m : compiled) {
                m.execute(caster, resolvedTarget);
            }
        }
        setCooldown(cdKey, skill.getCooldown());
    }

    private boolean evaluateConditions(List<String> conditions, LivingEntity caster, LivingEntity target) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (String cond : conditions) {
            String[] parts = cond.split(":");
            if (parts.length < 2) continue;
            String type = parts[0].toLowerCase(java.util.Locale.ROOT);
            String arg = parts[1];
            switch (type) {
                case "chance" -> {
                    double chance = Double.parseDouble(arg);
                    if (Math.random() * 100 > chance) return false;
                }
                case "health_below" -> {
                    double threshold = Double.parseDouble(arg);
                    if (caster.getHealth() > threshold) return false;
                }
                case "health_above" -> {
                    double threshold = Double.parseDouble(arg);
                    if (caster.getHealth() < threshold) return false;
                }
                case "has_item" -> {
                    if (!(caster instanceof Player)) return false;
                    boolean has = false;
                    for (org.bukkit.inventory.ItemStack item : ((Player) caster).getInventory().getContents()) {
                        if (item != null && plugin.getItemManager().getItemId(item) != null
                                && plugin.getItemManager().getItemId(item).equalsIgnoreCase(arg)) {
                            has = true; break;
                        }
                    }
                    if (!has) return false;
                }
                case "permission" -> {
                    if (!(caster instanceof Player)) return false;
                    if (!((Player) caster).hasPermission(arg)) return false;
                }
                case "time_day" -> {
                    if (caster.getWorld().getTime() > 13000) return false;
                }
                case "time_night" -> {
                    if (caster.getWorld().getTime() < 13000) return false;
                }
            }
        }
        return true;
    }

    private LivingEntity resolveTarget(List<String> targeters, LivingEntity caster, LivingEntity fallback) {
        if (targeters == null || targeters.isEmpty()) return fallback;
        for (String t : targeters) {
            String[] parts = t.split(":");
            String type = parts[0].toLowerCase(java.util.Locale.ROOT);
            switch (type) {
                case "self" -> { return caster; }
                case "target" -> { return fallback; }
                case "nearest" -> {
                    double radius = parts.length > 1 ? Double.parseDouble(parts[1]) : 10;
                    LivingEntity nearest = null;
                    double minDist = radius;
                    for (org.bukkit.entity.Entity e : caster.getNearbyEntities(radius, radius, radius)) {
                        if (e instanceof LivingEntity && e != caster) {
                            double dist = e.getLocation().distance(caster.getLocation());
                            if (dist < minDist) {
                                minDist = dist;
                                nearest = (LivingEntity) e;
                            }
                        }
                    }
                    return nearest != null ? nearest : fallback;
                }
            }
        }
        return fallback;
    }

    private boolean isOnCooldown(String key, double seconds) {
        if (seconds <= 0) return false;
        Long last = cooldowns.get(key);
        return last != null && System.currentTimeMillis() - last < seconds * 1000;
    }

    private void setCooldown(String key, double seconds) {
        if (seconds > 0) cooldowns.put(key, System.currentTimeMillis());
    }

    @Override
    public void reload() {
        registry.clear();
        mechanicCache.clear();
        cooldowns.clear();
        Map<String, YamlConfiguration> configs = plugin.getConfigManager().getConfigsByType("skills");
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            loadSkills(entry.getValue());
        }
        plugin.getLogger().info("[SkillManager] Loaded " + registry.size() + " skills");
    }

    private void loadSkills(YamlConfiguration config) {
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            try {
                String id = key;
                String name = section.getString("name", id);
                double cooldown = section.getDouble("cooldown", 0);
                double manaCost = section.getDouble("mana_cost", 0);
                List<String> conditions = section.getStringList("conditions");
                List<String> targeters = section.getStringList("targeters");
                List<String> mechanics = section.getStringList("mechanics");

                KiSkill skill = new KiSkill(id, name, cooldown, manaCost, conditions, targeters, mechanics);
                registry.register(id, skill);
                // Compile mechanics at load time
                mechanicCache.put(id, compileMechanics(mechanics));
            } catch (Exception e) {
                plugin.getLogger().warning("[SkillManager] Failed to load: " + key);
            }
        }
    }

    /** Compile string mechanics into executable objects at load time */
    private List<CompiledMechanic> compileMechanics(List<String> mechanics) {
        List<CompiledMechanic> result = new ArrayList<>();
        if (mechanics == null) return result;
        for (String m : mechanics) {
            String[] parts = m.split(":", 2);
            String type = parts[0].toLowerCase(java.util.Locale.ROOT);
            String args = parts.length > 1 ? parts[1] : "";
            result.add(new CompiledMechanic(type, args));
        }
        return result;
    }

    /** Pre-compiled mechanic for zero-parse execution */
    private class CompiledMechanic {
        final String type;
        final String args;

        CompiledMechanic(String type, String args) {
            this.type = type;
            this.args = args;
        }

        void execute(LivingEntity caster, LivingEntity target) {
            switch (type) {
                case "damage" -> {
                    double dmg = parseDouble(args, 5);
                    if (target != null) target.damage(dmg);
                }
                case "heal" -> {
                    double amount = parseDouble(args, 5);
                    var attr = caster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                    if (attr != null) {
                        caster.setHealth(Math.min(caster.getHealth() + amount, attr.getValue()));
                    }
                }
                case "particle" -> {
                    try {
                        Location loc = target != null ? target.getLocation() : caster.getLocation();
                        Particle p = Particle.valueOf(args.toUpperCase(java.util.Locale.ROOT));
                        loc.getWorld().spawnParticle(p, loc, 20, 0.5, 0.5, 0.5);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[Skill] Unknown particle: " + args);
                    }
                }
                case "sound" -> {
                    try {
                        Location loc = target != null ? target.getLocation() : caster.getLocation();
                        Sound s = Sound.valueOf(args.toUpperCase(java.util.Locale.ROOT));
                        loc.getWorld().playSound(loc, s, 1, 1);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[Skill] Unknown sound: " + args);
                    }
                }
                case "potion" -> {
                    String[] pArgs = args.split(":");
                    if (pArgs.length >= 2 && target != null) {
                        org.bukkit.potion.PotionEffectType pet = org.bukkit.potion.PotionEffectType.getByName(pArgs[0].toUpperCase(java.util.Locale.ROOT));
                        int duration = Integer.parseInt(pArgs[1]) * 20;
                        int amp = pArgs.length > 2 ? Integer.parseInt(pArgs[2]) : 0;
                        if (pet != null) target.addPotionEffect(new org.bukkit.potion.PotionEffect(pet, duration, amp));
                    }
                }
                case "command" -> {
                    String cmd = args.replace("{caster}", caster.getName())
                        .replace("{target}", target != null ? target.getName() : "");
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
                }
                case "message" -> {
                    if (caster instanceof Player) ((Player) caster).sendMessage(args.replace("&", "\u00a7"));
                }
                case "teleport" -> {
                    if (target != null) {
                        String[] tArgs = args.split(":");
                        if (tArgs.length >= 4) {
                            org.bukkit.World w = org.bukkit.Bukkit.getWorld(tArgs[0]);
                            if (w != null) {
                                target.teleport(new Location(w, Double.parseDouble(tArgs[1]),
                                    Double.parseDouble(tArgs[2]), Double.parseDouble(tArgs[3])));
                            }
                        }
                    }
                }
            }
        }

        double parseDouble(String s, double def) {
            try { return Double.parseDouble(s); } catch (Exception e) { return def; }
        }
    }
}
