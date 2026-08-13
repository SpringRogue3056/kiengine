package com.ki.engine.block.real;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

/**
 * 真实自定义方块数据 - 通过协议层实现无限方块类型
 */
public class RealBlockData {

    private final String id;
    private final Material visualMaterial; // 客户端看到的材质
    private final BlockData blockData;   // 服务器端的 BlockData
    private final boolean interactable;
    private final String dropItem;
    private final int customModelData;   // 资源包模型覆盖
    private Integer stateId;             // NMS BlockState ID

    public RealBlockData(String id, Material visualMaterial, BlockData blockData,
                         boolean interactable, String dropItem, int customModelData) {
        this.id = id;
        this.visualMaterial = visualMaterial;
        this.blockData = blockData;
        this.interactable = interactable;
        this.dropItem = dropItem;
        this.customModelData = customModelData;
    }

    public RealBlockData(String id, Material visualMaterial, BlockData blockData, boolean interactable) {
        this(id, visualMaterial, blockData, interactable, id, 0);
    }

    public String getId() { return id; }
    public Material getVisualMaterial() { return visualMaterial; }
    public BlockData getBlockData() { return blockData; }
    public boolean isInteractable() { return interactable; }
    public String getDropItem() { return dropItem; }
    public int getCustomModelData() { return customModelData; }
    public Integer getStateId() { return stateId; }
    public void setStateId(Integer stateId) { this.stateId = stateId; }
}
