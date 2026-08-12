package com.ki.engine.enchantment.effect;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 附魔效果接口 - 所有自定义附魔效果必须实现此接口
 * 支持上下文：施法者、目标、触发事件、附魔等级、额外参数
 */
public interface EnchantmentEffect {

    /**
     * 执行效果
     * @param caster 附魔持有者（玩家或实体）
     * @param target 目标实体（可能为null）
     * @param level 附魔等级
     * @param item 触发效果的物品
     * @param triggerEvent 触发此效果的原始Bukkit事件
     * @param params 效果参数（从配置解析）
     */
    void execute(LivingEntity caster, LivingEntity target, int level, ItemStack item,
                 Event triggerEvent, Map<String, String> params);

    /**
     * 效果类型标识，用于配置反序列化
     */
    String getTypeId();

    /**
     * 是否支持目标为空（如范围效果）
     */
    default boolean supportsNullTarget() { return false; }
}
