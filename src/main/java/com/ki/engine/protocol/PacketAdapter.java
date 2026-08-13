package com.ki.engine.protocol;

import java.util.HashSet;
import java.util.Set;

/**
 * 包适配器 - 简化版 PacketListener，只需重写关心的方法
 */
public abstract class PacketAdapter implements PacketListener {

    private final Set<Class<?>> packets = new HashSet<>();

    public PacketAdapter() {}

    public PacketAdapter(Class<?>... packetTypes) {
        for (Class<?> type : packetTypes) {
            packets.add(type);
        }
    }

    /**
     * 添加监听的包类型
     */
    public PacketAdapter addPacketType(Class<?> packetType) {
        packets.add(packetType);
        return this;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        // Override in subclass
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        // Override in subclass
    }

    @Override
    public Set<Class<?>> getListeningPackets() {
        return packets;
    }
}
