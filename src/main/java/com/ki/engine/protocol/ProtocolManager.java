package com.ki.engine.protocol;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ProtocolManager - 内嵌协议层，无需外部 ProtocolLib
 * 通过反射注入 Netty ChannelHandler 拦截 Minecraft 网络包
 */
public class ProtocolManager implements Manager {

    private final KiEnginePlugin plugin;
    private final Map<UUID, Object> playerChannels = new ConcurrentHashMap<>();
    private final List<PacketListener> listeners = new CopyOnWriteArrayList<>();
    private final String handlerName = "kiengine_protocol";
    private boolean initialized = false;

    private Class<?> packetClass;
    private Class<?> connectionClass;
    private Class<?> serverGamePacketListenerClass;
    private Class<?> channelClass;
    private Class<?> channelPipelineClass;
    private Class<?> channelDuplexHandlerClass;
    private Field connectionField;
    private Field channelField;

    public ProtocolManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        try {
            initReflection();
            injectAllPlayers();
            initialized = true;
            plugin.getLogger().info("[ProtocolManager] Injected into " + playerChannels.size() + " players");
        } catch (Exception e) {
            plugin.getLogger().severe("[ProtocolManager] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        for (UUID uuid : new ArrayList<>(playerChannels.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) uninjectPlayer(player);
        }
        playerChannels.clear();
        listeners.clear();
        initialized = false;
    }

    private void initReflection() throws Exception {
        packetClass = Class.forName("net.minecraft.network.protocol.Packet");
        connectionClass = Class.forName("net.minecraft.network.Connection");
        serverGamePacketListenerClass = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
        channelClass = Class.forName("io.netty.channel.Channel");
        channelPipelineClass = Class.forName("io.netty.channel.ChannelPipeline");
        channelDuplexHandlerClass = Class.forName("io.netty.channel.ChannelDuplexHandler");

        connectionField = serverGamePacketListenerClass.getDeclaredField("connection");
        connectionField.setAccessible(true);
        channelField = connectionClass.getDeclaredField("channel");
        channelField.setAccessible(true);
    }

    public void injectPlayer(Player player) {
        if (!initialized) return;
        try {
            Object channel = getChannel(player);
            if (channel == null) return;
            Object pipeline = channelClass.getMethod("pipeline").invoke(channel);
            Object existing = channelPipelineClass.getMethod("get", String.class).invoke(pipeline, handlerName);
            if (existing != null) {
                channelPipelineClass.getMethod("remove", String.class).invoke(pipeline, handlerName);
            }
            Object handler = createChannelHandler(player);
            channelPipelineClass.getMethod("addBefore", String.class, String.class, channelDuplexHandlerClass)
                .invoke(pipeline, "packet_handler", handlerName, handler);
            playerChannels.put(player.getUniqueId(), channel);
        } catch (Exception e) {
            plugin.getLogger().warning("[ProtocolManager] Failed to inject " + player.getName() + ": " + e.getMessage());
        }
    }

    public void uninjectPlayer(Player player) {
        Object channel = playerChannels.remove(player.getUniqueId());
        if (channel != null) {
            try {
                Object pipeline = channelClass.getMethod("pipeline").invoke(channel);
                Object existing = channelPipelineClass.getMethod("get", String.class).invoke(pipeline, handlerName);
                if (existing != null) {
                    channelPipelineClass.getMethod("remove", String.class).invoke(pipeline, handlerName);
                }
            } catch (Exception ignored) {}
        }
    }

    public void injectAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            injectPlayer(player);
        }
    }

    private Object getChannel(Player player) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = connectionField.get(handle);
        if (connection == null) return null;
        return channelField.get(connection);
    }

    private Object createChannelHandler(Player player) throws Exception {
        return Proxy.newProxyInstance(
            channelDuplexHandlerClass.getClassLoader(),
            new Class<?>[]{channelDuplexHandlerClass},
            new ChannelHandlerProxy(player)
        );
    }

    private class ChannelHandlerProxy implements InvocationHandler {
        private final Player player;
        ChannelHandlerProxy(Player player) { this.player = player; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("channelRead".equals(name)) {
                Object msg = args[1];
                if (packetClass.isInstance(msg)) {
                    PacketEvent event = new PacketEvent(player, msg, PacketEvent.PacketDirection.INCOMING);
                    for (PacketListener listener : listeners) {
                        if (listener.getListeningPackets().contains(msg.getClass())) {
                            try { listener.onPacketReceiving(event); } catch (Exception ignored) {}
                        }
                    }
                    if (!event.isCancelled()) return method.invoke(proxy, args);
                    return null;
                }
            } else if ("write".equals(name)) {
                Object msg = args[1];
                if (packetClass.isInstance(msg)) {
                    PacketEvent event = new PacketEvent(player, msg, PacketEvent.PacketDirection.OUTGOING);
                    for (PacketListener listener : listeners) {
                        if (listener.getListeningPackets().contains(msg.getClass())) {
                            try { listener.onPacketSending(event); } catch (Exception ignored) {}
                        }
                    }
                    if (!event.isCancelled()) {
                        args[1] = event.getPacket();
                        return method.invoke(proxy, args);
                    }
                    return null;
                }
            }
            return method.invoke(proxy, args);
        }
    }

    public void addPacketListener(PacketListener listener) { listeners.add(listener); }
    public void removePacketListener(PacketListener listener) { listeners.remove(listener); }
    public List<PacketListener> getPacketListeners() { return new ArrayList<>(listeners); }

    public void sendPacket(Player player, Object packet) {
        Object channel = playerChannels.get(player.getUniqueId());
        if (channel != null) {
            try {
                Object pipeline = channelClass.getMethod("pipeline").invoke(channel);
                channelPipelineClass.getMethod("writeAndFlush", Object.class).invoke(pipeline, packet);
            } catch (Exception ignored) {}
        }
    }

    public boolean isPacket(Object obj) { return packetClass != null && packetClass.isInstance(obj); }
    public String getPacketName(Object packet) { return packet != null ? packet.getClass().getSimpleName() : "null"; }
    public boolean isInitialized() { return initialized; }
}
