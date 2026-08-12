package com.ki.engine.block.noteblock;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NoteBlock-based real block registration system.
 * Uses NoteBlock's instrument (16 types) + note (25 values) to support up to 400 custom blocks.
 * No ProtocolLib required - works on Paper 1.21+ natively.
 */
public class NoteBlockManager {

    private final KiEnginePlugin plugin;
    private final Map<String, NoteBlockData> blockRegistry = new ConcurrentHashMap<>();
    private final Map<Location, String> placedBlocks = new ConcurrentHashMap<>();
    private int nextInstrument = 0;
    private int nextNote = 0;

    public NoteBlockManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadBlocks();
        plugin.getLogger().info("[NoteBlockManager] Registered " + blockRegistry.size() + " custom blocks");
    }

    private void loadBlocks() {
        Map<String, YamlConfiguration> blockConfigs = plugin.getConfigManager().getConfigsByType("blocks");
        for (Map.Entry<String, YamlConfiguration> entry : blockConfigs.entrySet()) {
            String blockId = entry.getKey();
            YamlConfiguration config = entry.getValue();
            if (!config.getBoolean("noteblock", false)) continue;

            NoteBlockData data = new NoteBlockData(
                blockId,
                config.getString("display_name", blockId),
                config.getString("material", "NOTE_BLOCK"),
                config.getInt("custom_model_data", 0),
                config.getBoolean("interactable", false),
                config.getString("drop_item", blockId)
            );
            registerBlock(data);
        }
    }

    public void registerBlock(NoteBlockData data) {
        if (nextInstrument >= Instrument.values().length) {
            plugin.getLogger().warning("[NoteBlockManager] Instrument limit reached! Cannot register: " + data.getId());
            return;
        }

        Instrument instrument = Instrument.values()[nextInstrument];
        Note note = new Note(nextNote);
        data.setInstrument(instrument);
        data.setNote(note);
        blockRegistry.put(data.getId(), data);

        nextNote++;
        if (nextNote >= 25) {
            nextNote = 0;
            nextInstrument++;
        }
    }

    public NoteBlockData getBlockData(String blockId) {
        return blockRegistry.get(blockId);
    }

    public NoteBlockData getBlockData(Location loc) {
        String blockId = placedBlocks.get(loc);
        return blockId != null ? blockRegistry.get(blockId) : null;
    }

    public NoteBlockData getBlockData(Block block) {
        if (block.getType() != Material.NOTE_BLOCK) return null;
        if (!(block.getBlockData() instanceof NoteBlock nb)) return null;
        return findByNoteBlock(nb);
    }

    private NoteBlockData findByNoteBlock(NoteBlock nb) {
        for (NoteBlockData data : blockRegistry.values()) {
            if (data.getInstrument() == nb.getInstrument() && data.getNote().equals(nb.getNote())) {
                return data;
            }
        }
        return null;
    }

    public void placeBlock(String blockId, Location loc) {
        NoteBlockData data = blockRegistry.get(blockId);
        if (data == null) return;

        Block block = loc.getBlock();
        block.setType(Material.NOTE_BLOCK);
        if (block.getBlockData() instanceof NoteBlock nb) {
            nb.setInstrument(data.getInstrument());
            nb.setNote(data.getNote());
            block.setBlockData(nb);
        }
        placedBlocks.put(loc, blockId);
    }

    public void removeBlock(Location loc) {
        placedBlocks.remove(loc);
    }

    public boolean isCustomBlock(Block block) {
        return getBlockData(block) != null;
    }

    public Map<String, NoteBlockData> getRegistry() {
        return new HashMap<>(blockRegistry);
    }

    public void shutdown() {
        placedBlocks.clear();
        blockRegistry.clear();
    }
}
