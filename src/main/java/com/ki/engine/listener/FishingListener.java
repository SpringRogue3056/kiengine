package com.ki.engine.listener;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 钓鱼监听器 - 支持稀有度、生物群系、品质系统
 */
public class FishingListener implements Listener {

    private final KiEnginePlugin plugin;
    private final Random random = new Random();

    // 鱼类定义：ID -> {稀有度, 生物群系, 基础价值}
    private final Map<String, FishData> fishDatabase = new HashMap<>();

    public FishingListener(KiEnginePlugin plugin) {
        this.plugin = plugin;
        initFishDatabase();
    }

    private void initFishDatabase() {
        fishDatabase.put("COD", new FishData("common", "all", 5.0));
        fishDatabase.put("SALMON", new FishData("common", "all", 8.0));
        fishDatabase.put("TROPICAL_FISH", new FishData("uncommon", "warm", 15.0));
        fishDatabase.put("PUFFERFISH", new FishData("rare", "warm", 25.0));
        fishDatabase.put("SQUID", new FishData("uncommon", "ocean", 12.0));
        fishDatabase.put("GLOW_SQUID", new FishData("rare", "deep", 30.0));
        fishDatabase.put("ANCIENT_FISH", new FishData("legendary", "deep", 100.0));
        fishDatabase.put("GOLDEN_KOI", new FishData("epic", "all", 80.0));
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        org.bukkit.entity.Entity caught = event.getCaught();
        if (!(caught instanceof org.bukkit.entity.Item)) return;

        // 根据玩家钓鱼等级和生物群系计算稀有度
        String biome = player.getLocation().getBlock().getBiome().name();
        int fishingLevel = getFishingLevel(player);

        // 生成自定义鱼
        String fishId = rollFish(biome, fishingLevel);
        if (fishId != null) {
            FishData data = fishDatabase.get(fishId);
            ItemStack customFish = createCustomFish(fishId, data, fishingLevel);
            ((org.bukkit.entity.Item) caught).setItemStack(customFish);

            String rarityColor = getRarityColor(data.rarity);
            player.sendMessage(rarityColor + "\u00a7l\u9493\u8d77\u4e86\uff01\u00a7r " + rarityColor + fishId);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);
        }
    }

    private String rollFish(String biome, int level) {
        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, FishData> entry : fishDatabase.entrySet()) {
            FishData data = entry.getValue();
            // 生物群系匹配
            if (!data.biome.equals("all") && !biome.toLowerCase().contains(data.biome)) continue;
            // 等级要求
            int requiredLevel = getRequiredLevel(data.rarity);
            if (level < requiredLevel) continue;
            // 根据稀有度加权
            int weight = getRarityWeight(data.rarity);
            for (int i = 0; i < weight; i++) {
                candidates.add(entry.getKey());
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    private ItemStack createCustomFish(String id, FishData data, int level) {
        ItemStack fish = new ItemStack(Material.COD);
        ItemMeta meta = fish.getItemMeta();
        if (meta != null) {
            String color = getRarityColor(data.rarity);
            meta.setDisplayName(color + id);
            List<String> lore = new ArrayList<>();
            lore.add("\u00a77\u7a00\u6709\u5ea6: " + color + data.rarity);
            lore.add("\u00a77\u4ef7\u503c: \u00a76$" + String.format("%.1f", data.value));
            lore.add("\u00a77\u751f\u7269\u7fa4\u7cfb: \u00a7f" + data.biome);
            lore.add("\u00a77\u4f7f\u7528 /ki sellfish \u51fa\u552e");
            meta.setLore(lore);
            fish.setItemMeta(meta);
        }
        return fish;
    }

    private int getFishingLevel(Player player) {
        return plugin.getRpgManager().getLevel(player, "fishing");
    }

    private int getRequiredLevel(String rarity) {
        return switch (rarity) {
            case "common" -> 1;
            case "uncommon" -> 5;
            case "rare" -> 15;
            case "epic" -> 30;
            case "legendary" -> 50;
            default -> 1;
        };
    }

    private int getRarityWeight(String rarity) {
        return switch (rarity) {
            case "common" -> 50;
            case "uncommon" -> 20;
            case "rare" -> 8;
            case "epic" -> 3;
            case "legendary" -> 1;
            default -> 10;
        };
    }

    private String getRarityColor(String rarity) {
        return switch (rarity) {
            case "common" -> "\u00a77";
            case "uncommon" -> "\u00a7a";
            case "rare" -> "\u00a79";
            case "epic" -> "\u00a75";
            case "legendary" -> "\u00a76";
            default -> "\u00a7f";
        };
    }

    private static class FishData {
        String rarity;
        String biome;
        double value;
        FishData(String rarity, String biome, double value) {
            this.rarity = rarity;
            this.biome = biome;
            this.value = value;
        }
    }
}
