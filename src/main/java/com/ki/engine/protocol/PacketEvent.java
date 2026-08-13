package com.ki.engine.protocol;

import org.bukkit.entity.Player;

/**
 * 包事件 - 封装被拦截的 Minecraft 网络包
 */
public class PacketEvent {

    public enum PacketDirection {
        INCOMING,   // 客户端 -> 服务器
        OUTGOING    // 服务器 -> 客户端
    }

    private final Player player;
    private Object packet;
    private final PacketDirection direction;
    private boolean cancelled = false;

    public PacketEvent(Player player, Object packet, PacketDirection direction) {
        this.player = player;
        this.packet = packet;
        this.direction = direction;
    }

    public Player getPlayer() { return player; }
    public Object getPacket() { return packet; }
    public void setPacket(Object packet) { this.packet = packet; }
    public PacketDirection getDirection() { return direction; }
    public boolean isIncoming() { return direction == PacketDirection.INCOMING; }
    public boolean isOutgoing() { return direction == PacketDirection.OUTGOING; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    /**
     * 获取包对象的字段值（反射）
     */
    public Object getFieldValue(String fieldName) {
        try {
            java.lang.reflect.Field field = packet.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(packet);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 设置包对象的字段值（反射）
     */
    public void setFieldValue(String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = packet.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(packet, value);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * 获取包类名
     */
    public String getPacketName() {
        return packet != null ? packet.getClass().getSimpleName() : "null";
    }
}
