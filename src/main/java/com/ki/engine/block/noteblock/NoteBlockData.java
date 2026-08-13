package com.ki.engine.block.noteblock;

import org.bukkit.Instrument;
import org.bukkit.Note;

/**
 * NoteBlock-based custom block data.
 * Uses instrument (16 types) + note (25 values) = 400 possible combinations.
 */
public class NoteBlockData {

    private final String id;
    private final String displayName;
    private final String material;
    private final int customModelData;
    private final boolean interactable;
    private final String dropItem;

    private Instrument instrument;
    private Note note;

    public NoteBlockData(String id, String displayName, String material,
                         int customModelData, boolean interactable, String dropItem) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.interactable = interactable;
        this.dropItem = dropItem;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public boolean isInteractable() { return interactable; }
    public String getDropItem() { return dropItem; }

    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }

    public Note getNote() { return note; }
    public void setNote(Note note) { this.note = note; }
}
