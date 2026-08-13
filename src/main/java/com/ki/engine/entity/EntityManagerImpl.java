package com.ki.engine.entity;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import com.ki.engine.event.KiMobSpawnEvent;
import com.ki.engine.registry.Registry;
import com.ki.engine.registry.SimpleRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe entity manager with safe shutdown and event firing.
 */
public class EntityManagerImpl implements EntityManager, Listener, Manager {
    private final KiEnginePlugin plugin;
    private final Registry<KiMob> registry = new SimpleRegistry<>();
    private final Map<UUID, String> activeMobs = new ConcurrentHashMap<>();
    private final NamespacedKey mobIdKey;
    private volatile boolean shuttingDown = false;

    public EntityManagerImpl(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.mobIdKey = new NamespacedKey(plugin, "ki_mob_id");
    }

    @Override
    public void init() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        reload();
    }

    @Override public Registry<KiMob> getMobRegistry() { return registry; }

    @Override
    public LivingEntity spawnMob(String id, Location loc) {
        if (shuttingDown) return null;
        KiMob mob = registry.get(id);
        if (mob == null) return null;
        LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, mob.getBaseType());
        mob.apply(entity);
        activeMobs.put(entity.getUniqueId(), id);
        entity.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, id);

        // Fire spawn event
        KiMobSpawnEvent event = new KiMobSpawnEvent(id, entity, loc);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            entity.remove();
            return null;
        }

        return entity;
    }

    @Override public String getMobId(UUID uuid) { return activeMobs.get(uuid); }
    @Override public void removeMob(UUID uuid) { activeMobs.remove(uuid); }

    @Override
    public void reload() {
        registry.clear();
        Map<String, YamlConfiguration> configs = plugin.getConfigManager().getConfigsByType("entities");
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            loadMobs(entry.getValue());
        }
        plugin.getLogger().info("[EntityManager] Loaded " + registry.size() + " mobs");
    }

    private void loadMobs(YamlConfiguration config) {
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            try {
                String id = key;
                org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(
                    section.getString("type", "ZOMBIE").toUpperCase(java.util.Locale.ROOT));
                String name = section.getString("name", id);
                double health = section.getDouble("health", 20);
                double damage = section.getDouble("damage", 5);
                double speed = section.getDouble("speed", 0.25);
                double armor = section.getDouble("armor", 0);
                List<String> skills = section.getStringList("skills");
                List<String> drops = section.getStringList("drops");
                boolean isNpc = section.getBoolean("npc", false);
                String interact = section.getString("interact_action", null);

                KiMob mob = new KiMob(id, type, name, health, damage, speed, armor, skills, drops, isNpc, interact);
                registry.register(id, mob);
            } catch (Exception e) {
                plugin.getLogger().warning("[EntityManager] Failed to load: " + key);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (shuttingDown) return;
        UUID uuid = event.getEntity().getUniqueId();
        String mobId = activeMobs.remove(uuid);
        if (mobId != null) {
            KiMob mob = registry.get(mobId);
            if (mob != null && !mob.getDrops().isEmpty()) {
                event.getDrops().clear();
                for (String dropId : mob.getDrops()) {
                    ItemStack drop = plugin.getItemManager().getItem(dropId);
                    if (drop != null) {
                        event.getEntity().getWorld().dropItemNaturally(
                            event.getEntity().getLocation(), drop);
                    }
                }
            }
        }
    }

    @Override
    public void shutdown() {
        shuttingDown = true;
        // Copy keys to avoid CME during iteration
        for (UUID uuid : new ArrayList<>(activeMobs.keySet())) {
            try {
                Entity entity = plugin.getServer().getEntity(uuid);
                if (entity != null) entity.remove();
            } catch (Exception ignored) {}
        }
        activeMobs.clear();
    }
}
