package com.ki.engine.particle;

import org.bukkit.Color;
import org.bukkit.Particle;

/**
 * 粒子效果数据定义 - 支持丰富的粒子参数配置
 */
public class ParticleEffectData {

    private final String id;
    private final Particle particle;
    private final int count;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double speed;
    private final Color color; // 仅对 REDSTONE/DUST 有效
    private final float size;  // 粒子大小
    private final ParticleShape shape;
    private final double radius;
    private final int points;
    private final long duration; // tick
    private final long interval; // tick

    public ParticleEffectData(String id, Particle particle, int count,
                               double offsetX, double offsetY, double offsetZ,
                               double speed, Color color, float size,
                               ParticleShape shape, double radius, int points,
                               long duration, long interval) {
        this.id = id;
        this.particle = particle;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.speed = speed;
        this.color = color;
        this.size = size;
        this.shape = shape;
        this.radius = radius;
        this.points = points;
        this.duration = duration;
        this.interval = interval;
    }

    public String getId() { return id; }
    public Particle getParticle() { return particle; }
    public int getCount() { return count; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public double getOffsetZ() { return offsetZ; }
    public double getSpeed() { return speed; }
    public Color getColor() { return color; }
    public float getSize() { return size; }
    public ParticleShape getShape() { return shape; }
    public double getRadius() { return radius; }
    public int getPoints() { return points; }
    public long getDuration() { return duration; }
    public long getInterval() { return interval; }

    /**
     * 从配置字符串快速构建
     * format: PARTICLE:FLAME,count:20,offset:0.5,speed:0.1,shape:CIRCLE,radius:2,points:16
     */
    public static ParticleEffectData fromString(String id, String config) {
        Particle particle = Particle.FLAME;
        int count = 20;
        double offset = 0.5, speed = 0.1;
        Color color = Color.RED;
        float size = 1.0f;
        ParticleShape shape = ParticleShape.POINT;
        double radius = 1.0;
        int points = 16;
        long duration = 1, interval = 1;

        for (String pair : config.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim().toLowerCase();
            String v = kv[1].trim();
            try {
                switch (k) {
                    case "particle" -> particle = Particle.valueOf(v.toUpperCase());
                    case "count" -> count = Integer.parseInt(v);
                    case "offset" -> offset = Double.parseDouble(v);
                    case "speed" -> speed = Double.parseDouble(v);
                    case "color" -> {
                        String[] rgb = v.split("-");
                        color = Color.fromRGB(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                    }
                    case "size" -> size = Float.parseFloat(v);
                    case "shape" -> shape = ParticleShape.valueOf(v.toUpperCase());
                    case "radius" -> radius = Double.parseDouble(v);
                    case "points" -> points = Integer.parseInt(v);
                    case "duration" -> duration = Long.parseLong(v);
                    case "interval" -> interval = Long.parseLong(v);
                }
            } catch (Exception ignored) {}
        }
        return new ParticleEffectData(id, particle, count, offset, offset, offset, speed, color, size, shape, radius, points, duration, interval);
    }
}
