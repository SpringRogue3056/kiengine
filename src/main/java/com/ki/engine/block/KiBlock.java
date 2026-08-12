package com.ki.engine.block;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * KiEngine 自定义方块定义
 * 支持多方块结构（如：篝火+坩埚=烹饪锅）
 */
public class KiBlock {
    private final String id;
    private final Material material;
    private final String displayName;
    private final boolean interactable;
    private final boolean isMultiblock;      // 是否为多方块结构的一部分
    private final String multiblockId;       // 所属多方块结构ID
    private final String multiblockRole;       // 在多方块中的角色（如 "base", "top"）

    public KiBlock(String id, Material material, String displayName,
                   boolean interactable, boolean isMultiblock,
                   String multiblockId, String multiblockRole) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.interactable = interactable;
        this.isMultiblock = isMultiblock;
        this.multiblockId = multiblockId;
        this.multiblockRole = multiblockRole;
    }

    public void place(Location loc) {
        loc.getBlock().setType(material);
    }

    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public boolean isInteractable() { return interactable; }
    public boolean isMultiblock() { return isMultiblock; }
    public String getMultiblockId() { return multiblockId; }
    public String getMultiblockRole() { return multiblockRole; }
}
