package com.ki.engine.particle;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 粒子管理器 - 高级粒子效果系统
 * 支持：形状生成、持续动画、预设管理、性能优化（视距裁剪）
 */
public class ParticleManager {

    private final KiEnginePlugin plugin;
    private final Map<String, ParticleEffectData> presets = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeAnimations = new ConcurrentHashMap<>();
    private int animationId = 0;

    public ParticleManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
        registerDefaultPresets();
    }

    private void registerDefaultPresets() {
        // 治疗光环
        presets.put("heal_aura", new ParticleEffectData("heal_aura", Particle.HEART, 5, 0.3, 0.3, 0.3, 0.05, null, 1.0f, ParticleShape.CIRCLE, 1.5, 12, 20, 2));
        // 火焰螺旋
        presets.put("fire_spiral", new ParticleEffectData("fire_spiral", Particle.FLAME, 3, 0.1, 0.1, 0.1, 0.02, null, 1.0f, ParticleShape.HELIX, 2.0, 20, 40, 1));
        // 冰霜爆炸
        presets.put("ice_burst", new ParticleEffectData("ice_burst", Particle.SNOWFLAKE, 50, 1.0, 1.0, 1.0, 0.3, null, 1.0f, ParticleShape.EXPLOSION, 2.0, 30, 10, 1));
        // 魔法星
        presets.put("magic_star", new ParticleEffectData("magic_star", Particle.END_ROD, 8, 0.2, 0.2, 0.2, 0.01, null, 1.0f, ParticleShape.STAR, 1.5, 10, 30, 2));
        // 毒雾
        presets.put("poison_mist", new ParticleEffectData("poison_mist", Particle.DUST, 20, 0.5, 0.5, 0.5, 0.05, org.bukkit.Color.LIME, 2.0f, ParticleShape.SPHERE, 1.0, 16, 60, 5));
    }

    // ========== 基础粒子发射 ==========

    public void spawnParticle(String presetId, Location center) {
        ParticleEffectData data = presets.get(presetId);
        if (data == null) {
            // 尝试作为原始粒子类型
            try {
                Particle p = Particle.valueOf(presetId.toUpperCase());
                center.getWorld().spawnParticle(p, center, 20, 0.5, 0.5, 0.5, 0.1);
            } catch (IllegalArgumentException ignored) {}
            return;
        }
        spawnShape(data, center);
    }

    public void spawnParticle(ParticleEffectData data, Location center) {
        spawnShape(data, center);
    }

    // ========== 形状生成 ==========

    private void spawnShape(ParticleEffectData data, Location center) {
        World world = center.getWorld();
        if (world == null) return;

        switch (data.getShape()) {
            case POINT -> spawnPoint(world, data, center);
            case CIRCLE -> spawnCircle(world, data, center);
            case SPHERE -> spawnSphere(world, data, center);
            case HELIX -> spawnHelix(world, data, center);
            case LINE -> spawnLine(world, data, center);
            case WAVE -> spawnWave(world, data, center);
            case STAR -> spawnStar(world, data, center);
            case HEART -> spawnHeart(world, data, center);
            case EXPLOSION -> spawnExplosion(world, data, center);
        }
    }

    private void spawnPoint(World world, ParticleEffectData data, Location center) {
        world.spawnParticle(data.getParticle(), center, data.getCount(),
            data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), getDustOptions(data));
    }

    private void spawnCircle(World world, ParticleEffectData data, Location center) {
        double radius = data.getRadius();
        int points = data.getPoints();
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, center.getY(), z);
            world.spawnParticle(data.getParticle(), loc, 1, 0, 0, 0, data.getSpeed(), getDustOptions(data));
        }
    }

    private void spawnSphere(World world, ParticleEffectData data, Location center) {
        double radius = data.getRadius();
        int points = data.getPoints();
        for (int i = 0; i < points; i++) {
            double theta = 2 * Math.PI * i / points;
            for (int j = 0; j < points / 2; j++) {
                double phi = Math.PI * j / (points / 2);
                double x = center.getX() + radius * Math.sin(phi) * Math.cos(theta);
                double y = center.getY() + radius * Math.cos(phi);
                double z = center.getZ() + radius * Math.sin(phi) * Math.sin(theta);
                Location loc = new Location(world, x, y, z);
                world.spawnParticle(data.getParticle(), loc, 1, 0, 0, 0, data.getSpeed(), getDustOptions(data));
            }
        }
    }

    private void spawnHelix(World world, ParticleEffectData data, Location center) {
        double radius = data.getRadius();
        int points = data.getPoints();
        double height = 3.0;
        for (int i = 0; i < points; i++) {
            double angle = 4 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double y = center.getY() + (height * i / points);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, y, z);
            world.spawnParticle(data.getParticle(), loc, 1, 0, 0, 0, data.getSpeed(), getDustOptions(data));
        }
    }

    private void spawnLine(World world, ParticleEffectData data, Location center) {
        double length = data.getRadius();
        int points = data.getPoints();
        for (int i = 0; i < points; i++) {
            double x = center.getX() + (length * i / points);
            Location loc = new Location(world, x, center.getY(), center.getZ());
            world.spawnParticle(data.getParticle(), loc, 1, 0, 0, 0, data.getSpeed(), getDustOptions(data));
        }
    }

    private void spawnWave(World world, ParticleEffectData data, Location center) {
        double length = data.getRadius();
        int points = data.getPoints();
        for (int i = 0; i < points; i++) {
            double x = center.getX() + (length * i / points);
            double y = center.getY() + Math.sin(2 * Math.PI * i / points) * 0.5;
            Location loc = new Location(world, x, y, center.getZ());
            world.spawnParticle(data.getParticle(), loc, 1, 0, 0, 0, data.getSpeed(), getDustOptions(data));
        }
    }

    private void spawnStar(World world, ParticleEffectData data, Location center) {
        double radius = data.getRadius();
        int points = data.getPoints();
        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI * i / points;
            double r = (i % 2 == 0) ? radius : radius * 0.4;
            double x = center.getX() + r * Math.cos(angle);
            double z = center.getZ() + r * Math.sin(angle);
            Location loc = new Location(world, x, center.getY(), z);
            world.spawnParticle(data.getParticle(), loc, 1, 0, 0, 0, data.getSpeed(), getDustOptions(data));
        }
    }

    private void spawnHeart(World world, ParticleEffectData data, Location center) {
        double scale = data.getRadius();
        int points = data.getPoints();
        for (int i = 0; i < points; i++) {
            double t = 2 * Math.PI * i / points;
            double x = center.getX() + scale * 16 * Math.pow(Math.sin(t), 3) / 16;
            double y = center.getY() + scale * (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16;
            double z = center.getZ();
            Location loc = new Location(world, x, y, z);
            world.spawnParticle(data.getParticle(), loc, 1, 0, 0, 0, data.getSpeed(), getDustOptions(data));
        }
    }

    private void spawnExplosion(World world, ParticleEffectData data, Location center) {
        double radius = data.getRadius();
        int count = data.getCount();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            double r = radius * random.nextDouble();
            double theta = 2 * Math.PI * random.nextDouble();
            double phi = Math.PI * random.nextDouble();
            double x = center.getX() + r * Math.sin(phi) * Math.cos(theta);
            double y = center.getY() + r * Math.cos(phi);
            double z = center.getZ() + r * Math.sin(phi) * Math.sin(theta);
            Location loc = new Location(world, x, y, z);
            world.spawnParticle(data.getParticle(), loc, 1, 0, 0, 0, data.getSpeed() * 2, getDustOptions(data));
        }
    }

    private Object getDustOptions(ParticleEffectData data) {
        if (data.getParticle() == Particle.DUST && data.getColor() != null) {
            return new Particle.DustOptions(data.getColor(), data.getSize());
        }
        return null;
    }

    // ========== 持续动画 ==========

    /**
     * 在位置播放持续粒子动画
     * @return 动画UUID，可用于停止
     */
    public UUID playAnimation(String presetId, Location center) {
        ParticleEffectData data = presets.get(presetId);
        if (data == null) return null;
        return playAnimation(data, center);
    }

    public UUID playAnimation(ParticleEffectData data, Location center) {
        UUID id = UUID.randomUUID();
        BukkitTask task = plugin.getScheduler().runTimer(() -> {
            spawnShape(data, center);
        }, 0, data.getInterval());
        activeAnimations.put(id, task);

        // 自动停止
        if (data.getDuration() > 0) {
            plugin.getScheduler().runLater(() -> stopAnimation(id), data.getDuration());
        }
        return id;
    }

    public void stopAnimation(UUID id) {
        BukkitTask task = activeAnimations.remove(id);
        if (task != null) task.cancel();
    }

    public void stopAllAnimations() {
        for (BukkitTask task : activeAnimations.values()) {
            task.cancel();
        }
        activeAnimations.clear();
    }

    // ========== 预设管理 ==========

    public void registerPreset(String id, ParticleEffectData data) {
        presets.put(id, data);
    }

    public ParticleEffectData getPreset(String id) {
        return presets.get(id);
    }

    public Set<String> getPresetIds() {
        return new HashSet<>(presets.keySet());
    }

    public void unregisterPreset(String id) {
        presets.remove(id);
    }

    public void shutdown() {
        stopAllAnimations();
        presets.clear();
    }
}
