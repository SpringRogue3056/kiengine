package com.ki.engine.block;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import com.ki.engine.registry.Registry;
import com.ki.engine.registry.SimpleRegistry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

/**
 * 方块管理器实现 - 多方块结构检测与自定义方块
 */
public class BlockManagerImpl implements BlockManager, Manager {

    private final KiEnginePlugin plugin;
    private final Registry<KiBlock> registry = new SimpleRegistry<>();
    private final NamespacedKey blockIdKey;
    private final BlockDataStorage storage;

    public BlockManagerImpl(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.blockIdKey = new NamespacedKey(plugin, "ki_block_id");
        this.storage = new BlockDataStorage(plugin);
    }

    @Override
    public void init() {
        reload();
    }

    @Override
    public Registry<KiBlock> getRegistry() {
        return registry;
    }

    @Override
    public void placeBlock(String id, Location loc) {
        KiBlock block = registry.get(id);
        if (block == null) return;
        block.place(loc);
        storage.setBlock(loc, id);
    }

    @Override
    public String getBlockId(Location loc) {
        String stored = storage.getBlock(loc);
        if (stored != null) return stored;
        if (isCookingPot(loc)) return "cooking_pot";
        if (isCuttingBoard(loc)) return "cutting_board";
        return null;
    }

    @Override
    public void removeBlock(Location loc) {
        storage.removeBlock(loc);
    }

    @Override
    public boolean isCustomBlock(Location loc) {
        return getBlockId(loc) != null;
    }

    @Override
    public void reload() {
        registry.clear();
        Map<String, YamlConfiguration> configs = plugin.getConfigManager().getConfigsByType("blocks");
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            loadBlocks(entry.getValue());
        }
        plugin.getLogger().info("[BlockManager] 已加载 " + registry.keys().size() + " 个方块定义");
    }

    private void loadBlocks(YamlConfiguration config) {
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            try {
                String id = key;
                Material material = Material.valueOf(section.getString("material", "STONE").toUpperCase());
                String name = section.getString("name", id);
                boolean interactable = section.getBoolean("interactable", false);
                boolean isMultiblock = section.getBoolean("multiblock", false);
                String multiblockId = section.getString("multiblock_id", null);
                String multiblockRole = section.getString("multiblock_role", null);

                KiBlock block = new KiBlock(id, material, name, interactable, isMultiblock, multiblockId, multiblockRole);
                registry.register(id, block);
            } catch (Exception e) {
                plugin.getLogger().warning("[BlockManager] 加载方块失败: " + key);
            }
        }
    }

    // 检测烹饪锅多方块结构：篝火（点燃）+ 上面坩埚
    public boolean isCookingPot(Location loc) {
        if (loc.getBlock().getType() != Material.CAULDRON) return false;
        org.bukkit.block.Block below = loc.clone().subtract(0, 1, 0).getBlock();
        if (below.getType() != Material.CAMPFIRE && below.getType() != Material.SOUL_CAMPFIRE) return false;
        if (below.getBlockData() instanceof org.bukkit.block.data.Lightable) {
            return ((org.bukkit.block.data.Lightable) below.getBlockData()).isLit();
        }
        return false;
    }

    // 检测砧板：橡木原木（带PDC标记）
    public boolean isCuttingBoard(Location loc) {
        if (loc.getBlock().getType() != Material.OAK_LOG && loc.getBlock().getType() != Material.STRIPPED_OAK_LOG) return false;
        // TODO: 检查PDC标记
        return false;
    }

    public boolean isCampfireLit(Location loc) {
        org.bukkit.block.Block block = loc.getBlock();
        if (block.getType() != Material.CAMPFIRE && block.getType() != Material.SOUL_CAMPFIRE) return false;
        if (block.getBlockData() instanceof org.bukkit.block.data.Lightable) {
            return ((org.bukkit.block.data.Lightable) block.getBlockData()).isLit();
        }
        return false;
    }
}
