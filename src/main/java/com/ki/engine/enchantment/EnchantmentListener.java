package com.ki.engine.enchantment;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.enchantment.effect.EnchantmentEffect;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 附魔事件监听器 - 监听所有游戏事件并触发对应的附魔效果
 * 支持主手/副手/护甲槽位检测，PDC存储动态附魔
 */
public class EnchantmentListener implements Listener {

    private final KiEnginePlugin plugin;
    private final EnchantmentManager manager;
    /** PDC key for dynamic enchantments on items */
    private final org.bukkit.NamespacedKey enchantKey;
    /** Periodic effect tracking: playerUuid -> taskId */
    private final Map<UUID, Integer> periodicTasks = new ConcurrentHashMap<>();

    public EnchantmentListener(KiEnginePlugin plugin, EnchantmentManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.enchantKey = new org.bukkit.NamespacedKey(plugin, "ki_enchants");
    }

    // ========== 攻击事件 ==========

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 攻击者附魔 (ON_HIT)
        if (event.getDamager() instanceof LivingEntity attacker) {
            ItemStack weapon = getMainHandItem(attacker);
            triggerEnchantments(attacker, (LivingEntity) event.getEntity(), weapon,
                    KiEnchantment.TriggerType.ON_HIT, event);
        }
        // 被攻击者附魔 (ON_HIT_BY)
        if (event.getEntity() instanceof LivingEntity victim) {
            for (ItemStack armor : getArmorItems(victim)) {
                triggerEnchantments(victim, (LivingEntity) event.getDamager(), armor,
                        KiEnchantment.TriggerType.ON_HIT_BY, event);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.getKiller() != null) {
            ItemStack weapon = getMainHandItem(victim.getKiller());
            triggerEnchantments(victim.getKiller(), victim, weapon,
                    KiEnchantment.TriggerType.ON_KILL, event);
        }
    }

    // ========== 挖掘/放置 ==========

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        triggerEnchantments(event.getPlayer(), null, tool,
                KiEnchantment.TriggerType.ON_MINE, event);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        triggerEnchantments(event.getPlayer(), null, item,
                KiEnchantment.TriggerType.ON_BLOCK_PLACE, event);
    }

    // ========== 使用/交互 ==========

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT")) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() != Material.AIR) {
                triggerEnchantments(event.getPlayer(), null, item,
                        KiEnchantment.TriggerType.ON_USE, event);
            }
        }
    }

    // ========== 弹射物 ==========

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof LivingEntity target) {
            Projectile projectile = event.getEntity();
            if (projectile.getShooter() instanceof LivingEntity shooter) {
                ItemStack weapon = getMainHandItem(shooter);
                triggerEnchantments(shooter, target, weapon,
                        KiEnchantment.TriggerType.ON_PROJECTILE_HIT, event);
            }
        }
    }

    // ========== 钓鱼 ==========

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            ItemStack rod = event.getPlayer().getInventory().getItemInMainHand();
            triggerEnchantments(event.getPlayer(), null, rod,
                    KiEnchantment.TriggerType.ON_FISH, event);
        }
    }

    // ========== 装备/手持 ==========

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        // 停止旧物品的周期性效果
        stopPeriodicEffects(player);

        // 触发 ON_HELD
        if (newItem != null && newItem.getType() != Material.AIR) {
            triggerEnchantments(player, null, newItem, KiEnchantment.TriggerType.ON_HELD, event);
            // 启动周期性效果
            startPeriodicEffects(player, newItem);
        }
    }

    // ========== 移动相关 ==========

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            Player player = event.getPlayer();
            ItemStack item = getMainHandItem(player);
            triggerEnchantments(player, null, item, KiEnchantment.TriggerType.ON_SNEAK, event);
            for (ItemStack armor : getArmorItems(player)) {
                triggerEnchantments(player, null, armor, KiEnchantment.TriggerType.ON_SNEAK, event);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (event.isSprinting()) {
            Player player = event.getPlayer();
            ItemStack item = getMainHandItem(player);
            triggerEnchantments(player, null, item, KiEnchantment.TriggerType.ON_SPRINT, event);
            for (ItemStack armor : getArmorItems(player)) {
                triggerEnchantments(player, null, armor, KiEnchantment.TriggerType.ON_SPRINT, event);
            }
        }
    }

    // ========== 核心触发逻辑 ==========

    /**
     * 触发物品上所有符合条件的附魔
     */
    private void triggerEnchantments(LivingEntity caster, LivingEntity target, ItemStack item,
                                     KiEnchantment.TriggerType trigger, Event event) {
        if (item == null || item.getType() == Material.AIR) return;

        Map<String, Integer> enchants = getEnchantmentsOnItem(item);
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            String enchantId = entry.getKey();
            int level = entry.getValue();
            KiEnchantment enchant = manager.getRegistry().get(enchantId);
            if (enchant == null) continue;
            if (!enchant.getTriggers().contains(trigger)) continue;

            executeEnchantEffects(caster, target, level, item, enchant, event);
        }
    }

    /**
     * 执行附魔的所有效果
     */
    private void executeEnchantEffects(LivingEntity caster, LivingEntity target, int level,
                                       ItemStack item, KiEnchantment enchant, Event event) {
        for (String effectStr : enchant.getEffects()) {
            String[] parts = effectStr.split("\\|", 2);
            String effectType = parts[0].trim().toUpperCase(java.util.Locale.ROOT);
            String paramStr = parts.length > 1 ? parts[1] : "";

            EnchantmentEffect effect = manager.getEffect(effectType);
            if (effect == null) continue;
            if (target == null && !effect.supportsNullTarget()) continue;

            Map<String, String> params = EnchantmentManager.parseEffectParams(paramStr);
            try {
                effect.execute(caster, target, level, item, event, params);
            } catch (Exception e) {
                plugin.getLogger().warning("[Enchantment] Effect " + effectType + " failed: " + e.getMessage());
            }
        }
    }

    // ========== 附魔数据存取 ==========

    /**
     * 获取物品上的所有附魔（配置预置 + PDC动态）
     */
    public Map<String, Integer> getEnchantmentsOnItem(ItemStack item) {
        Map<String, Integer> result = new HashMap<>();
        if (item == null || !item.hasItemMeta()) return result;

        // 1. 从配置获取预置附魔（自定义物品）
        String itemId = plugin.getItemManager().getItemId(item);
        if (itemId != null) {
            result.putAll(manager.getItemEnchantments(itemId));
        }

        // 2. 从PDC获取动态附魔（玩家通过附魔台/命令添加的）
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String enchantData = pdc.get(enchantKey, PersistentDataType.STRING);
            if (enchantData != null) {
                for (String entry : enchantData.split(",")) {
                    String[] kv = entry.split(":");
                    if (kv.length == 2) {
                        try {
                            result.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return result;
    }

    /**
     * 给物品添加动态附魔（通过命令/API）
     */
    public boolean addEnchantment(ItemStack item, String enchantId, int level) {
        if (item == null || !item.hasItemMeta()) return false;
        KiEnchantment enchant = manager.getRegistry().get(enchantId);
        if (enchant == null) return false;
        if (level < 1 || level > enchant.getMaxLevel()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Map<String, Integer> current = new HashMap<>();
        String existing = pdc.get(enchantKey, PersistentDataType.STRING);
        if (existing != null) {
            for (String entry : existing.split(",")) {
                String[] kv = entry.split(":");
                if (kv.length == 2) {
                    try { current.put(kv[0].trim(), Integer.parseInt(kv[1].trim())); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        current.put(enchantId, level);

        // 序列化回PDC
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : current.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(e.getKey()).append(":").append(e.getValue());
        }
        pdc.set(enchantKey, PersistentDataType.STRING, sb.toString());
        item.setItemMeta(meta);

        // 更新Lore显示
        updateEnchantmentLore(item, current);
        return true;
    }

    /**
     * 从物品移除动态附魔
     */
    public boolean removeEnchantment(ItemStack item, String enchantId) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String existing = pdc.get(enchantKey, PersistentDataType.STRING);
        if (existing == null) return false;

        Map<String, Integer> current = new HashMap<>();
        for (String entry : existing.split(",")) {
            String[] kv = entry.split(":");
            if (kv.length == 2 && !kv[0].trim().equalsIgnoreCase(enchantId)) {
                try { current.put(kv[0].trim(), Integer.parseInt(kv[1].trim())); }
                catch (NumberFormatException ignored) {}
            }
        }

        if (current.isEmpty()) {
            pdc.remove(enchantKey);
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : current.entrySet()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(e.getKey()).append(":").append(e.getValue());
            }
            pdc.set(enchantKey, PersistentDataType.STRING, sb.toString());
        }
        item.setItemMeta(meta);
        updateEnchantmentLore(item, current);
        return true;
    }

    /**
     * 更新物品Lore显示附魔信息
     */
    private void updateEnchantmentLore(ItemStack item, Map<String, Integer> enchants) {
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        // 移除旧的附魔Lore
        lore.removeIf(line -> line.startsWith("\u00a77\u00a7o") || line.startsWith("\u00a7r\u00a77Enchanted:"));

        // 添加新的附魔Lore
        if (!enchants.isEmpty()) {
            lore.add("");
            lore.add("\u00a7r\u00a77Enchanted:");
            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                KiEnchantment enchant = manager.getRegistry().get(entry.getKey());
                if (enchant != null && !enchant.isHidden()) {
                    String color = enchant.getRarityColor();
                    lore.add(color + "\u00a7o" + enchant.getDisplayName(entry.getValue()));
                }
            }
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    // ========== 周期性效果 ==========

    private void startPeriodicEffects(Player player, ItemStack item) {
        Map<String, Integer> enchants = getEnchantmentsOnItem(item);
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            KiEnchantment enchant = manager.getRegistry().get(entry.getKey());
            if (enchant == null) continue;
            if (!enchant.getTriggers().contains(KiEnchantment.TriggerType.PERIODIC)) continue;

            int interval = 20; // 默认1秒
            // 从效果参数中读取间隔
            for (String effectStr : enchant.getEffects()) {
                Map<String, String> params = EnchantmentManager.parseEffectParams(
                        effectStr.contains("|") ? effectStr.split("\\|", 2)[1] : "");
                if (params.containsKey("interval")) {
                    try { interval = Integer.parseInt(params.get("interval")) * 20; }
                    catch (NumberFormatException ignored) {}
                }
            }

            int taskId = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !player.getInventory().getItemInMainHand().equals(item)) {
                        cancel();
                        return;
                    }
                    executeEnchantEffects(player, null, entry.getValue(), item, enchant, null);
                }
            }.runTaskTimer(plugin, interval, interval).getTaskId();

            periodicTasks.put(player.getUniqueId(), taskId);
        }
    }

    private void stopPeriodicEffects(Player player) {
        Integer taskId = periodicTasks.remove(player.getUniqueId());
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
    }

    // ========== 工具方法 ==========

    private ItemStack getMainHandItem(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getInventory().getItemInMainHand();
        }
        return null;
    }

    private List<ItemStack> getArmorItems(LivingEntity entity) {
        List<ItemStack> armor = new ArrayList<>();
        if (entity instanceof Player player) {
            org.bukkit.inventory.PlayerInventory inv = player.getInventory();
            for (ItemStack item : new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}) {
                if (item != null && item.getType() != Material.AIR) armor.add(item);
            }
        }
        return armor;
    }
}
