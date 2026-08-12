package com.ki.engine.resourcepack;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/**
 * 资源包管理器 - 自动生成并下发资源包
 * 支持：CustomModelData模型、自定义字体、自定义音效
 */
public class ResourcePackManager implements Manager {

    private final KiEnginePlugin plugin;
    private final File packDir;
    private final File outputDir;
    private String packUrl;
    private String packHash;
    private PackHttpServer httpServer;

    public ResourcePackManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.packDir = new File(plugin.getDataFolder(), "resourcepack");
        this.outputDir = new File(plugin.getDataFolder(), "output");
    }

    @Override
    public void init() {
        reload();
        // Start embedded HTTP server for auto-delivery
        httpServer = new PackHttpServer(plugin);
        httpServer.start();
        plugin.getServer().getPluginManager().registerEvents(httpServer, plugin);
    }

    @Override
    public void reload() {
        generatePack();
        if (httpServer != null) {
            httpServer.pushToAllPlayers();
        }
    }

    @Override
    public void shutdown() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    /**
     * 扫描 items/ 目录下的所有自定义物品配置，生成资源包
     */
    public void generatePack() {
        try {
            packDir.mkdirs();
            outputDir.mkdirs();

            // 创建临时目录结构
            File tempDir = new File(outputDir, "temp_pack");
            deleteDirectory(tempDir);
            tempDir.mkdirs();

            // assets/minecraft/models/item/
            File modelsDir = new File(tempDir, "assets/minecraft/models/item");
            modelsDir.mkdirs();

            // assets/minecraft/textures/item/
            File texturesDir = new File(tempDir, "assets/minecraft/textures/item");
            texturesDir.mkdirs();

            // 生成 mcmeta
            File mcmeta = new File(tempDir, "pack.mcmeta");
            writeFile(mcmeta, "{\n  \"pack\": {\n    \"pack_format\": 34,\n    \"description\": \"KiEngine Resource Pack\"\n  }\n}");

            // 扫描物品配置生成模型覆盖
            generateItemModels(modelsDir, texturesDir);

            // 打包为 zip
            File zipFile = new File(outputDir, "KiEngine-ResourcePack.zip");
            zipDirectory(tempDir, zipFile);

            // 计算 SHA1
            this.packHash = computeSha1(zipFile);

            plugin.getLogger().info("[ResourcePack] Generated: " + zipFile.getName() + " (SHA1: " + packHash + ")");

            // 清理临时目录
            deleteDirectory(tempDir);

        } catch (Exception e) {
            plugin.getLogger().warning("[ResourcePack] Generation failed: " + e.getMessage());
        }
    }

    private void generateItemModels(File modelsDir, File texturesDir) {
        // 读取所有物品配置，为每个带 custom_model_data 的物品生成模型
        File itemsDir = new File(plugin.getDataFolder(), "packs/default/items");
        if (!itemsDir.exists()) return;

        // 基础物品模型覆盖映射
        Map<String, List<ModelOverride>> overrides = new HashMap<>();

        for (File file : itemsDir.listFiles(f -> f.getName().endsWith(".yml"))) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String key : config.getKeys(false)) {
                int cmd = config.getInt(key + ".custom_model_data", 0);
                String material = config.getString(key + ".material", "STONE").toLowerCase();
                if (cmd > 0) {
                    overrides.computeIfAbsent(material, k -> new ArrayList<>())
                            .add(new ModelOverride(cmd, "kiengine:item/" + key));
                }
            }
        }

        // 写入模型覆盖文件
        for (Map.Entry<String, List<ModelOverride>> entry : overrides.entrySet()) {
            String material = entry.getKey();
            List<ModelOverride> list = entry.getValue();
            list.sort(Comparator.comparingInt(o -> o.predicate));

            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"parent\": \"minecraft:item/generated\",\n");
            sb.append("  \"textures\": {\"layer0\": \"minecraft:item/").append(material).append("\"},\n");
            sb.append("  \"overrides\": [\n");
            for (int i = 0; i < list.size(); i++) {
                ModelOverride o = list.get(i);
                sb.append("    {\"predicate\": {\"custom_model_data\": ").append(o.predicate).append("}, \"model\": \"").append(o.model).append("\"}");
                if (i < list.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]\n}");

            writeFile(new File(modelsDir, material + ".json"), sb.toString());
        }
    }

    private void zipDirectory(File source, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Files.walk(source.toPath()).forEach(path -> {
                try {
                    String zipEntryName = source.toPath().relativize(path).toString().replace("\\", "/");
                    if (zipEntryName.isEmpty()) return;
                    if (Files.isDirectory(path)) {
                        zos.putNextEntry(new ZipEntry(zipEntryName + "/"));
                        zos.closeEntry();
                    } else {
                        zos.putNextEntry(new ZipEntry(zipEntryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private String computeSha1(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void writeFile(File file, String content) {
        try {
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("[ResourcePack] Write failed: " + file.getName());
        }
    }

    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    public String getPackUrl() { return packUrl; }
    public void setPackUrl(String url) { this.packUrl = url; }
    public String getPackHash() { return packHash; }
    public File getPackFile() { return new File(outputDir, "KiEngine-ResourcePack.zip"); }

    private record ModelOverride(int predicate, String model) {}
}
