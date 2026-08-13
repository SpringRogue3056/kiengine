package com.ki.engine.resourcepack;

import com.ki.engine.core.KiEnginePlugin;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;

/**
 * 内嵌HTTP服务器 - 自动提供资源包下载并下发给玩家
 * 无需外部Web服务器或Nginx
 */
public class PackHttpServer implements Listener {

    private final KiEnginePlugin plugin;
    private HttpServer server;
    private int port;
    private String baseUrl;

    public PackHttpServer(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        port = plugin.getConfigManager().getConfig("config") != null
                ? plugin.getConfigManager().getConfig("config").getInt("resourcepack.port", 25566)
                : 25566;

        String ip = plugin.getServer().getIp();
        if (ip == null || ip.isEmpty()) ip = "0.0.0.0";
        baseUrl = "http://" + ip + ":" + port + "/KiEngine-ResourcePack.zip";

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/KiEngine-ResourcePack.zip", new PackHandler());
            server.setExecutor(null);
            server.start();
            plugin.getLogger().info("[PackHttpServer] Started on port " + port);
        } catch (IOException e) {
            plugin.getLogger().warning("[PackHttpServer] Failed to start: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("[PackHttpServer] Stopped");
        }
    }

    /**
     * 当资源包生成完成后，向所有在线玩家推送
     */
    public void pushToAllPlayers() {
        ResourcePackManager rpm = plugin.getResourcePackManager();
        if (rpm == null || rpm.getPackHash() == null) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendPack(player);
        }
    }

    public void sendPack(Player player) {
        ResourcePackManager rpm = plugin.getResourcePackManager();
        if (rpm == null || rpm.getPackHash() == null) return;

        String url = rpm.getPackUrl() != null ? rpm.getPackUrl() : baseUrl;
        String hash = rpm.getPackHash();

        try {
            player.setResourcePack(url, hash.getBytes());
            plugin.getLogger().info("[PackHttpServer] Sent resource pack to " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("[PackHttpServer] Failed to send pack to " + player.getName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ResourcePackManager rpm = plugin.getResourcePackManager();
        if (rpm == null || rpm.getPackHash() == null) return;

        // 延迟2秒发送，确保玩家完全进入
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                sendPack(event.getPlayer());
            }
        }, 40L);
    }

    private class PackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ResourcePackManager rpm = plugin.getResourcePackManager();
            if (rpm == null || !rpm.getPackFile().exists()) {
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
                return;
            }

            File packFile = rpm.getPackFile();
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, packFile.length());

            try (OutputStream os = exchange.getResponseBody();
                 InputStream is = Files.newInputStream(packFile.toPath())) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    os.write(buffer, 0, read);
                }
            }
            exchange.close();
        }
    }

    public int getPort() { return port; }
    public String getBaseUrl() { return baseUrl; }
}
