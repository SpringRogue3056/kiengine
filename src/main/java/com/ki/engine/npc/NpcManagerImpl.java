package com.ki.engine.npc;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import com.ki.engine.entity.KiMob;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NpcManagerImpl implements NpcManager, Manager {
    private final KiEnginePlugin plugin;
    private final Map<UUID, NpcData> npcs = new HashMap<>();

    public NpcManagerImpl(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public UUID spawnNpc(String mobId, Location loc, String hologramText) {
        KiMob mob = plugin.getEntityManager().getMobRegistry().get(mobId);
        if (mob == null || !mob.isNpc()) return null;

        LivingEntity entity = plugin.getEntityManager().spawnMob(mobId, loc);
        if (entity == null) return null;

        UUID uuid = entity.getUniqueId();
        NpcData data = new NpcData(uuid, mobId, hologramText);

        if (hologramText != null && !hologramText.isEmpty()) {
            ArmorStand hologram = (ArmorStand) loc.getWorld().spawnEntity(
                loc.clone().add(0, 2.2, 0), EntityType.ARMOR_STAND);
            hologram.setVisible(false);
            hologram.setCustomNameVisible(true);
            hologram.setCustomName(hologramText.replace("&", "\u00a7"));
            hologram.setGravity(false);
            hologram.setInvulnerable(true);
            hologram.setMarker(true);
            data.hologramId = hologram.getUniqueId();
        }

        npcs.put(uuid, data);
        return uuid;
    }

    @Override public void setNpcPath(UUID npcId, List<Location> path) {
        NpcData data = npcs.get(npcId);
        if (data == null) return;
        data.path = path;
        data.pathIndex = 0;
    }

    @Override
    public void removeNpc(UUID npcId) {
        NpcData data = npcs.remove(npcId);
        if (data == null) return;
        LivingEntity entity = (LivingEntity) plugin.getServer().getEntity(npcId);
        if (entity != null) entity.remove();
        if (data.hologramId != null) {
            Entity holo = plugin.getServer().getEntity(data.hologramId);
            if (holo != null) holo.remove();
        }
    }

    @Override public void init() { reload(); }
    @Override public void reload() {}

    @Override
    public void shutdown() {
        for (UUID uuid : new ArrayList<>(npcs.keySet())) {
            removeNpc(uuid);
        }
    }

    private static class NpcData {
        final UUID entityId;
        final String mobId;
        final String hologramText;
        UUID hologramId;
        List<Location> path;
        int pathIndex = 0;

        NpcData(UUID entityId, String mobId, String hologramText) {
            this.entityId = entityId;
            this.mobId = mobId;
            this.hologramText = hologramText;
        }
    }
}
