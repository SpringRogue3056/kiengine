package com.ki.engine.protocol;

import java.util.Set;

/**
 * 包监听器接口 - 监听特定类型的 Minecraft 网络包
 */
public interface PacketListener {

    /**
     * 服务器发送包给客户端时触发
     */
    void onPacketSending(PacketEvent event);

    /**
     * 客户端发送包给服务器时触发
     */
    void onPacketReceiving(PacketEvent event);

    /**
     * 返回此监听器关心的包类型列表
     * 返回空列表表示监听所有包（性能开销大）
     */
    Set<Class<?>> getListeningPackets();
}
