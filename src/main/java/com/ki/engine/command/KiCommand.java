package com.ki.engine.command;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.item.KiItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * KiEngine main command with full subcommands and tab completion
 */
public class KiCommand implements CommandExecutor, TabCompleter {

    private final KiEnginePlugin plugin;

    public KiCommand(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("\u00a7e======== KiEngine v" + plugin.getDescription().getVersion() + " ========");
            sender.sendMessage("\u00a7e/ki menu      \u00a77- Open main menu");
            sender.sendMessage("\u00a7e/ki reload   \u00a77- Reload configuration");
            sender.sendMessage("\u00a7e/ki give <id> [amount] [player]  \u00a77- Give custom item");
            sender.sendMessage("\u00a7e/ki list     \u00a77- List all custom items");
            sender.sendMessage("\u00a7e/ki mob <id> \u00a77- Spawn custom mob");
            sender.sendMessage("\u00a7e/ki npc <id> \u00a77- Spawn NPC");
            sender.sendMessage("\u00a7e/ki skill <id> \u00a77- Cast skill (test)");
            sender.sendMessage("\u00a7e/ki status  \u00a77- View engine status");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("\u00a7cOnly players can use this");
                    return true;
                }
                plugin.getGuiManager().openMainMenu((Player) sender);
            }
            case "reload" -> {
                if (!sender.hasPermission("ki.reload")) {
                    sender.sendMessage("\u00a7cNo permission");
                    return true;
                }
                plugin.getConfigManager().reload();
                plugin.getItemManager().reload();
                plugin.getBlockManager().reload();
                plugin.getRecipeManager().reload();
                plugin.getEntityManager().reload();
                plugin.getSkillManager().reload();
                plugin.getRpgManager().reload();
                plugin.getFarmersDelightManager().reload();
                plugin.getGuiManager().getMenuBridge().regenerateMenus();
                sender.sendMessage("\u00a7aKiEngine reloaded");
            }
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "mob" -> handleMob(sender, args);
            case "npc" -> handleNpc(sender, args);
            case "skill" -> handleSkill(sender, args);
            case "status" -> handleStatus(sender);
            default -> sender.sendMessage("\u00a7cUnknown command. Use /ki for help");
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ki.give")) {
            sender.sendMessage("\u00a7cNo permission");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /ki give <id> [amount] [player]");
            return;
        }
        String itemId = args[1];
        int amount = 1;
        if (args.length >= 3) {
            try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                sender.sendMessage("\u00a7cAmount must be a number");
                return;
            }
        }
        Player target = null;
        if (args.length >= 4) {
            target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                sender.sendMessage("\u00a7cPlayer not found");
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        }
        if (target == null) {
            sender.sendMessage("\u00a7cPlease specify a player");
            return;
        }

        ItemStack item = plugin.getItemManager().getItem(itemId, amount);
        if (item == null) {
            sender.sendMessage("\u00a7cItem not found: " + itemId);
            return;
        }
        target.getInventory().addItem(item);
        sender.sendMessage("\u00a7aGave " + target.getName() + " " + amount + "x " + itemId);
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage("\u00a7e======== Custom Items ========");
        for (KiItem item : plugin.getItemManager().getRegistry().values()) {
            sender.sendMessage("\u00a77- \u00a7f" + item.getId() + " \u00a77(" + item.getDisplayName() + "\u00a77)");
        }
        sender.sendMessage("\u00a7eTotal: " + plugin.getItemManager().getRegistry().keys().size());
    }

    private void handleMob(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ki.admin")) {
            sender.sendMessage("\u00a7cNo permission");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /ki mob <id>");
            return;
        }
        Player player = (Player) sender;
        var entity = plugin.getEntityManager().spawnMob(args[1], player.getLocation());
        if (entity != null) {
            sender.sendMessage("\u00a7aSummoned: " + args[1]);
        } else {
            sender.sendMessage("\u00a7cMob not found: " + args[1]);
        }
    }

    private void handleNpc(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ki.admin")) {
            sender.sendMessage("\u00a7cNo permission");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /ki npc <mobId> [hologram]");
            return;
        }
        Player player = (Player) sender;
        String hologram = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : null;
        java.util.UUID npcId = plugin.getNpcManager().spawnNpc(args[1], player.getLocation(), hologram);
        if (npcId != null) {
            sender.sendMessage("\u00a7aSpawned NPC: " + args[1]);
        } else {
            sender.sendMessage("\u00a7cNPC not found or not NPC type: " + args[1]);
        }
    }

    private void handleSkill(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /ki skill <id>");
            return;
        }
        Player player = (Player) sender;
        plugin.getSkillManager().castSkill(args[1], player, player);
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage("\u00a7e======== KiEngine Status ========");
        sender.sendMessage("\u00a77Version: \u00a7f" + plugin.getDescription().getVersion());
        sender.sendMessage("\u00a77Items: \u00a7f" + plugin.getItemManager().getRegistry().keys().size());
        sender.sendMessage("\u00a77Recipes: \u00a7f" + plugin.getRecipeManager().getRegistry().keys().size());
        sender.sendMessage("\u00a77Mobs: \u00a7f" + plugin.getEntityManager().getMobRegistry().keys().size());
        sender.sendMessage("\u00a77Skills: \u00a7f" + plugin.getSkillManager().getRegistry().keys().size());
        sender.sendMessage("\u00a77KaMenu: \u00a7f" + (plugin.getGuiManager().getMenuBridge().isKaMenuAvailable() ? "\u00a7aConnected" : "\u00a7cNot found"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("menu", "reload", "give", "list", "mob", "npc", "skill", "status"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(plugin.getItemManager().getRegistry().keys());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("mob")) {
            completions.addAll(plugin.getEntityManager().getMobRegistry().keys());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("npc")) {
            for (var mob : plugin.getEntityManager().getMobRegistry().values()) {
                if (mob.isNpc()) completions.add(mob.getId());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("skill")) {
            completions.addAll(plugin.getSkillManager().getRegistry().keys());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.add("1"); completions.add("16"); completions.add("64");
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
        }
        return completions;
    }
}
