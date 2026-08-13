package com.ki.engine.gui.kamenu;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * KiEngine 动作处理器 - 向 KaMenu 注册 ki: 命名空间
 *
 * 使 KaMenu 的 YAML 菜单配置可以通过动作字符串调用 KiEngine 的所有功能：
 *
 *   ki:give <itemId> [amount]          - 给予玩家自定义物品
 *   ki:give_silent <itemId> [amount]   - 静默给予（无消息）
 *   ki:mob <mobId>                     - 在玩家位置召唤实体
 *   ki:npc <npcId> [hologram]          - 召唤NPC
 *   ki:skill <skillId> [target]        - 释放技能
 *   ki:skill_self <skillId>            - 对自己释放技能
 *   ki:exp <skill> <amount>            - 给予经验
 *   ki:menu <menuId> [args...]         - 打开另一个 KiEngine 菜单
 *   ki:command <cmd>                   - 以玩家身份执行命令
 *   ki:console <cmd>                   - 以控制台身份执行命令
 *   ki:message <msg>                   - 发送消息给玩家
 *   ki:broadcast <msg>                 - 广播消息
 *   ki:sound <sound> [volume] [pitch]  - 播放音效
 *   ki:particle <particle>             - 播放粒子效果
 *   ki:effect <effect> <duration> <amp> - 给予药水效果
 *   ki:teleport <world> <x> <y> <z>    - 传送玩家
 *   ki:close                           - 关闭当前菜单
 *   ki:reload                          - 重载 KiEngine（OP only）
 */
public class KiMenuActionHandler {

    private final KiEnginePlugin plugin;
    private static final String NAMESPACE = "ki";

    public KiMenuActionHandler(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        try {
            Class<?> apiClass = Class.forName("org.katacr.kamenu.api.KaMenuAPI");
            Class<?> handlerClass = Class.forName("org.katacr.kamenu.api.KaMenuActionHandler");

            // 创建匿名实现
            Object handler = java.lang.reflect.Proxy.newProxyInstance(
                handlerClass.getClassLoader(),
                new Class<?>[]{handlerClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("handle")) {
                        Player player = (Player) args[0];
                        String payload = (String) args[1];
                        handleKiAction(player, payload);
                        return null;
                    }
                    return null;
                }
            );

            java.lang.reflect.Method registerMethod = apiClass.getMethod(
                "registerActionHandler", String.class, handlerClass);
            registerMethod.invoke(null, NAMESPACE, handler);

            plugin.getLogger().info("[KiMenu] Registered 'ki:' action namespace with KaMenu");
        } catch (Exception e) {
            plugin.getLogger().warning("[KiMenu] Failed to register action handler: " + e.getMessage());
        }
    }

    public void unregister() {
        try {
            Class<?> apiClass = Class.forName("org.katacr.kamenu.api.KaMenuAPI");
            java.lang.reflect.Method method = apiClass.getMethod("unregisterActionHandler", String.class);
            method.invoke(null, NAMESPACE);
        } catch (Exception e) {
            plugin.getLogger().warning("[KiMenu] Failed to unregister action handler: " + e.getMessage());
        }
    }

    private void handleKiAction(Player player, String payload) {
        String[] parts = payload.trim().split("\\s+", 2);
        String action = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (action) {
            case "give" -> handleGive(player, args, false);
            case "give_silent" -> handleGive(player, args, true);
            case "mob" -> handleMob(player, args);
            case "npc" -> handleNpc(player, args);
            case "skill" -> handleSkill(player, args);
            case "skill_self" -> handleSkillSelf(player, args);
            case "exp" -> handleExp(player, args);
            case "menu" -> handleMenu(player, args);
            case "command" -> player.performCommand(args);
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), args.replace("{player}", player.getName()));
            case "message" -> player.sendMessage(args.replace("&", "\u00a7"));
            case "broadcast" -> Bukkit.broadcastMessage(args.replace("&", "\u00a7"));
            case "sound" -> handleSound(player, args);
            case "particle" -> handleParticle(player, args);
            case "effect" -> handleEffect(player, args);
            case "teleport" -> handleTeleport(player, args);
            case "close" -> player.closeInventory();
            case "reload" -> {
                if (player.hasPermission("ki.reload")) {
                    plugin.getConfigManager().reload();
                    plugin.getItemManager().reload();
                    plugin.getRecipeManager().reload();
                    plugin.getEntityManager().reload();
                    player.sendMessage("\u00a7aKiEngine reloaded.");
                }
            }
            default -> plugin.getLogger().warning("[KiMenu] Unknown action: ki:" + action);
        }
    }

    private void handleGive(Player player, String args, boolean silent) {
        String[] parts = args.split("\\s+");
        String itemId = parts[0];
        int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        org.bukkit.inventory.ItemStack item = plugin.getItemManager().getItem(itemId, amount);
        if (item != null) {
            player.getInventory().addItem(item);
            if (!silent) {
                player.sendMessage("\u00a7aReceived " + amount + "x " + itemId);
            }
        }
    }

    private void handleMob(Player player, String mobId) {
        var entity = plugin.getEntityManager().spawnMob(mobId, player.getLocation());
        if (entity != null) {
            player.sendMessage("\u00a7aSummoned: " + mobId);
        }
    }

    private void handleNpc(Player player, String args) {
        String[] parts = args.split("\\s+", 2);
        String npcId = parts[0];
        String hologram = parts.length > 1 ? parts[1] : null;
        plugin.getNpcManager().spawnNpc(npcId, player.getLocation(), hologram);
    }

    private void handleSkill(Player player, String args) {
        String[] parts = args.split("\\s+");
        String skillId = parts[0];
        plugin.getSkillManager().castSkill(skillId, player, player);
    }

    private void handleSkillSelf(Player player, String skillId) {
        plugin.getSkillManager().castSkill(skillId, player, player);
    }

    private void handleExp(Player player, String args) {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) return;
        plugin.getRpgManager().addExp(player, parts[0], Double.parseDouble(parts[1]));
    }

    private void handleMenu(Player player, String args) {
        String[] parts = args.split("\\s+");
        String menuId = parts[0];
        plugin.getGuiManager().getMenuBridge().openKaMenuDirect(player, menuId, parts);
    }

    private void handleSound(Player player, String args) {
        String[] parts = args.split("\\s+");
        org.bukkit.Sound sound = org.bukkit.Sound.valueOf(parts[0].toUpperCase());
        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void handleParticle(Player player, String args) {
        String[] parts = args.split("\\s+");
        org.bukkit.Particle particle = org.bukkit.Particle.valueOf(parts[0].toUpperCase());
        int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
        player.getWorld().spawnParticle(particle, player.getLocation(), count, 0.5, 0.5, 0.5);
    }

    private void handleEffect(Player player, String args) {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) return;
        org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(parts[0].toUpperCase());
        int duration = Integer.parseInt(parts[1]) * 20;
        int amp = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        if (type != null) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(type, duration, amp));
        }
    }

    private void handleTeleport(Player player, String args) {
        String[] parts = args.split("\\s+");
        if (parts.length < 4) return;
        org.bukkit.World world = Bukkit.getWorld(parts[0]);
        if (world == null) return;
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        player.teleport(new org.bukkit.Location(world, x, y, z));
    }
}
