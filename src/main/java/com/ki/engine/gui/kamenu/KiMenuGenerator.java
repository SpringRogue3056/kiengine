package com.ki.engine.gui.kamenu;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.item.KiItem;
import com.ki.engine.recipe.KiRecipe;
import com.ki.engine.entity.KiMob;
import com.ki.engine.skill.KiSkill;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * KiEngine 动态菜单生成器
 *
 * 根据 KiEngine 已加载的内容（items, recipes, mobs, skills）
 * 动态生成 KaMenu 兼容的 YAML 菜单配置，写入 KaMenu 的 menus 目录。
 *
 * 生成的菜单类型：
 *   - kiengine/main_menu.yml      : 主菜单（物品/配方/实体/技能入口）
 *   - kiengine/item_compendium.yml  : 物品图鉴（分页浏览所有自定义物品）
 *   - kiengine/recipe_browser.yml   : 配方浏览器
 *   - kiengine/mob_compendium.yml   : 实体图鉴
 *   - kiengine/skill_panel.yml      : 技能面板
 *   - kiengine/rpg_status.yml       : RPG 状态面板
 *   - kiengine/cooking_pot.yml      : 烹饪锅交互界面
 *   - kiengine/npc_dialog.yml       : NPC 对话模板
 */
public class KiMenuGenerator {

    private final KiEnginePlugin plugin;
    private File kaMenuMenusDir;

    public KiMenuGenerator(KiEnginePlugin plugin) {
        this.plugin = plugin;
        this.kaMenuMenusDir = new File(plugin.getDataFolder().getParentFile(), "KaMenu/menus");
    }

    /**
     * 生成所有动态菜单
     */
    public void generateAll() {
        if (!kaMenuMenusDir.exists()) {
            // 尝试查找 KaMenu 的数据目录
            File kaMenuDir = findKaMenuDir();
            if (kaMenuDir != null) {
                kaMenuMenusDir = new File(kaMenuDir, "menus");
            } else {
                plugin.getLogger().warning("[KiMenu] KaMenu menus directory not found. Skipping menu generation.");
                return;
            }
        }

        File kiengineDir = new File(kaMenuMenusDir, "kiengine");
        kiengineDir.mkdirs();

        generateMainMenu(kiengineDir);
        generateItemCompendium(kiengineDir);
        generateRecipeBrowser(kiengineDir);
        generateMobCompendium(kiengineDir);
        generateSkillPanel(kiengineDir);
        generateRpgStatus(kiengineDir);
        generateCookingPot(kiengineDir);
        generateNpcDialog(kiengineDir);

        plugin.getLogger().info("[KiMenu] Generated 8 dynamic menus to KaMenu/menus/kiengine/");
    }

    private File findKaMenuDir() {
        File pluginsDir = plugin.getDataFolder().getParentFile();
        File candidate = new File(pluginsDir, "KaMenu");
        if (candidate.exists()) return candidate;
        // 尝试其他可能的位置
        for (File f : pluginsDir.listFiles()) {
            if (f.isDirectory() && f.getName().toLowerCase().contains("kamenu")) {
                return f;
            }
        }
        return null;
    }

    // ==================== 主菜单 ====================
    private void generateMainMenu(File dir) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# KiEngine Main Menu - Auto-generated\n");
        yaml.append("title: '&6KiEngine &7- Main Menu'\n");
        yaml.append("rows: 5\n\n");
        yaml.append("layout:\n");
        yaml.append("  - '#########'\n");
        yaml.append("  - '# A B C #'\n");
        yaml.append("  - '# D E F #'\n");
        yaml.append("  - '# G H I #'\n");
        yaml.append("  - '#########'\n\n");
        yaml.append("items:\n");
        yaml.append("  '#':\n    material: BLACK_STAINED_GLASS_PANE\n    name: ' '&n\n\n");
        yaml.append("  'A':\n    material: CHEST\n    name: '&eItem Compendium'\n    lore:\n      - '&7Browse all custom items'\n      - '&7Total: &f").append(plugin.getItemManager().getRegistry().keys().size()).append("'\n    actions:\n      - 'menu:kiengine/item_compendium'\n\n");
        yaml.append("  'B':\n    material: CRAFTING_TABLE\n    name: '&eRecipe Browser'\n    lore:\n      - '&7View all recipes'\n      - '&7Total: &f").append(plugin.getRecipeManager().getRegistry().keys().size()).append("'\n    actions:\n      - 'menu:kiengine/recipe_browser'\n\n");
        yaml.append("  'C':\n    material: ZOMBIE_SPAWN_EGG\n    name: '&eMob Compendium'\n    lore:\n      - '&7View all custom mobs'\n      - '&7Total: &f").append(plugin.getEntityManager().getMobRegistry().keys().size()).append("'\n    actions:\n      - 'menu:kiengine/mob_compendium'\n\n");
        yaml.append("  'D':\n    material: ENCHANTED_BOOK\n    name: '&eSkill Panel'\n    lore:\n      - '&7View and use skills'\n      - '&7Total: &f").append(plugin.getSkillManager().getRegistry().keys().size()).append("'\n    actions:\n      - 'menu:kiengine/skill_panel'\n\n");
        yaml.append("  'E':\n    material: EXPERIENCE_BOTTLE\n    name: '&eRPG Status'\n    lore:\n      - '&7View your levels and stats'\n    actions:\n      - 'menu:kiengine/rpg_status'\n\n");
        yaml.append("  'F':\n    material: CAULDRON\n    name: '&eCooking Pot'\n    lore:\n      - '&7Open cooking interface'\n    actions:\n      - 'ki:menu cooking_pot'\n\n");
        yaml.append("  'G':\n    material: COMMAND_BLOCK\n    name: '&cAdmin'\n    lore:\n      - '&7Reload KiEngine'\n    actions:\n      - 'condition:perm ki.reload'\n      - 'ki:reload'\n      - 'message:&aKiEngine reloaded!'\n      - 'close'\n\n");
        yaml.append("  'H':\n    material: BARRIER\n    name: '&cClose'\n    actions:\n      - 'close'\n\n");
        yaml.append("  'I':\n    material: PLAYER_HEAD\n    name: '&ePlayer Info'\n    lore:\n      - '&7Name: &f%player_name%'\n      - '&7Level: &f%ki_level%'\n    actions:\n      - 'message:&7Your info has been displayed'\n\n");

        saveYaml(dir, "main_menu.yml", yaml.toString());
    }

    // ==================== 物品图鉴 ====================
    private void generateItemCompendium(File dir) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# KiEngine Item Compendium - Auto-generated\n");
        yaml.append("title: '&6Item Compendium'\n");
        yaml.append("rows: 6\n\n");
        yaml.append("layout:\n");
        yaml.append("  - '#########'\n");
        yaml.append("  - '#iiiiiii#'\n");
        yaml.append("  - '#iiiiiii#'\n");
        yaml.append("  - '#iiiiiii#'\n");
        yaml.append("  - '#iiiiiii#'\n");
        yaml.append("  - '###P#N###'\n\n");
        yaml.append("items:\n");
        yaml.append("  '#':\n    material: BLACK_STAINED_GLASS_PANE\n    name: ' '&n\n\n");
        yaml.append("  'P':\n    material: ARROW\n    name: '&aPrevious Page'\n    actions:\n      - 'page:prev'\n\n");
        yaml.append("  'N':\n    material: ARROW\n    name: '&aNext Page'\n    actions:\n      - 'page:next'\n\n");

        List<String> itemIds = new ArrayList<>(plugin.getItemManager().getRegistry().keys());
        int slotIndex = 0;
        for (String itemId : itemIds) {
            KiItem item = plugin.getItemManager().getRegistry().get(itemId);
            if (item == null) continue;
            char slotChar = (char) ('a' + (slotIndex % 26));
            yaml.append("  '").append(slotChar).append("':\n");
            yaml.append("    material: ").append(item.getMaterial().name()).append("\n");
            yaml.append("    name: '").append(item.getDisplayName().replace("'", "''")).append("'\n");
            yaml.append("    lore:\n");
            yaml.append("      - '&7ID: &f").append(itemId).append("'\n");
            yaml.append("      - '&7Type: &f").append(item.getMaterial().name()).append("'\n");
            if (item.isEdible()) {
                yaml.append("      - '&7Food: &f").append(item.getFoodLevel()).append(" | Sat: ").append(item.getSaturation()).append("'\n");
            }
            yaml.append("    actions:\n");
            yaml.append("      - 'ki:give ").append(itemId).append(" 1'\n");
            yaml.append("      - 'message:&aReceived 1x ").append(itemId).append("'\n");
            yaml.append("      - 'sound:ENTITY_ITEM_PICKUP'\n\n");
            slotIndex++;
            if (slotIndex >= 26) break; // 最多26个物品一页
        }

        saveYaml(dir, "item_compendium.yml", yaml.toString());
    }

    // ==================== 配方浏览器 ====================
    private void generateRecipeBrowser(File dir) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# KiEngine Recipe Browser - Auto-generated\n");
        yaml.append("title: '&6Recipe Browser'\n");
        yaml.append("rows: 6\n\n");
        yaml.append("layout:\n");
        yaml.append("  - '#########'\n");
        yaml.append("  - '#rrrrrrr#'\n");
        yaml.append("  - '#rrrrrrr#'\n");
        yaml.append("  - '#rrrrrrr#'\n");
        yaml.append("  - '#rrrrrrr#'\n");
        yaml.append("  - '###B#####'\n\n");
        yaml.append("items:\n");
        yaml.append("  '#':\n    material: BLACK_STAINED_GLASS_PANE\n    name: ' '&n\n\n");
        yaml.append("  'B':\n    material: BARRIER\n    name: '&cBack'\n    actions:\n      - 'menu:kiengine/main_menu'\n\n");

        List<String> recipeIds = new ArrayList<>(plugin.getRecipeManager().getRegistry().keys());
        int slotIndex = 0;
        for (String recipeId : recipeIds) {
            KiRecipe recipe = plugin.getRecipeManager().getRegistry().get(recipeId);
            if (recipe == null) continue;
            char slotChar = (char) ('a' + (slotIndex % 26));
            yaml.append("  '").append(slotChar).append("':\n");
            yaml.append("    material: CRAFTING_TABLE\n");
            yaml.append("    name: '&e").append(recipeId).append("'\n");
            yaml.append("    lore:\n");
            yaml.append("      - '&7Type: &f").append(recipe.getType()).append("'\n");
            yaml.append("      - '&7Result: &f").append(recipe.getResultId()).append(" x").append(recipe.getResultAmount()).append("'\n");
            yaml.append("      - '&7Ingredients:'\n");
            for (String ing : recipe.getIngredients()) {
                yaml.append("      - '&f  - ").append(ing).append("'\n");
            }
            if (recipe.getToolId() != null) {
                yaml.append("      - '&7Tool: &f").append(recipe.getToolId()).append("'\n");
            }
            yaml.append("    actions:\n");
            yaml.append("      - 'message:&7Recipe: ").append(recipeId).append("'\n\n");
            slotIndex++;
            if (slotIndex >= 26) break;
        }

        saveYaml(dir, "recipe_browser.yml", yaml.toString());
    }

    // ==================== 实体图鉴 ====================
    private void generateMobCompendium(File dir) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# KiEngine Mob Compendium - Auto-generated\n");
        yaml.append("title: '&6Mob Compendium'\n");
        yaml.append("rows: 6\n\n");
        yaml.append("layout:\n");
        yaml.append("  - '#########'\n");
        yaml.append("  - '#mmmmmm#'\n");
        yaml.append("  - '#mmmmmm#'\n");
        yaml.append("  - '#mmmmmm#'\n");
        yaml.append("  - '#mmmmmm#'\n");
        yaml.append("  - '###B#####'\n\n");
        yaml.append("items:\n");
        yaml.append("  '#':\n    material: BLACK_STAINED_GLASS_PANE\n    name: ' '&n\n\n");
        yaml.append("  'B':\n    material: BARRIER\n    name: '&cBack'\n    actions:\n      - 'menu:kiengine/main_menu'\n\n");

        List<String> mobIds = new ArrayList<>(plugin.getEntityManager().getMobRegistry().keys());
        int slotIndex = 0;
        for (String mobId : mobIds) {
            KiMob mob = plugin.getEntityManager().getMobRegistry().get(mobId);
            if (mob == null) continue;
            char slotChar = (char) ('a' + (slotIndex % 26));
            yaml.append("  '").append(slotChar).append("':\n");
            yaml.append("    material: ZOMBIE_SPAWN_EGG\n");
            yaml.append("    name: '").append(mob.getDisplayName().replace("'", "''")).append("'\n");
            yaml.append("    lore:\n");
            yaml.append("      - '&7ID: &f").append(mobId).append("'\n");
            yaml.append("      - '&7Type: &f").append(mob.getBaseType().name()).append("'\n");
            yaml.append("      - '&7HP: &c").append(mob.getMaxHealth()).append(" &7| DMG: &c").append(mob.getDamage()).append("'\n");
            yaml.append("      - '&7Speed: &f").append(mob.getSpeed()).append(" &7| Armor: &f").append(mob.getArmor()).append("'\n");
            if (mob.isNpc()) {
                yaml.append("      - '&a&lNPC'\n");
            }
            yaml.append("    actions:\n");
            yaml.append("      - 'ki:mob ").append(mobId).append("'\n");
            yaml.append("      - 'message:&aSummoned: ").append(mobId).append("'\n");
            yaml.append("      - 'sound:ENTITY_ZOMBIE_AMBIENT'\n\n");
            slotIndex++;
            if (slotIndex >= 26) break;
        }

        saveYaml(dir, "mob_compendium.yml", yaml.toString());
    }

    // ==================== 技能面板 ====================
    private void generateSkillPanel(File dir) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# KiEngine Skill Panel - Auto-generated\n");
        yaml.append("title: '&6Skill Panel'\n");
        yaml.append("rows: 6\n\n");
        yaml.append("layout:\n");
        yaml.append("  - '#########'\n");
        yaml.append("  - '#sssssss#'\n");
        yaml.append("  - '#sssssss#'\n");
        yaml.append("  - '#sssssss#'\n");
        yaml.append("  - '#sssssss#'\n");
        yaml.append("  - '###B#####'\n\n");
        yaml.append("items:\n");
        yaml.append("  '#':\n    material: BLACK_STAINED_GLASS_PANE\n    name: ' '&n\n\n");
        yaml.append("  'B':\n    material: BARRIER\n    name: '&cBack'\n    actions:\n      - 'menu:kiengine/main_menu'\n\n");

        List<String> skillIds = new ArrayList<>(plugin.getSkillManager().getRegistry().keys());
        int slotIndex = 0;
        for (String skillId : skillIds) {
            KiSkill skill = plugin.getSkillManager().getRegistry().get(skillId);
            if (skill == null) continue;
            char slotChar = (char) ('a' + (slotIndex % 26));
            yaml.append("  '").append(slotChar).append("':\n");
            yaml.append("    material: ENCHANTED_BOOK\n");
            yaml.append("    name: '&e").append(skill.getDisplayName().replace("'", "''")).append("'\n");
            yaml.append("    lore:\n");
            yaml.append("      - '&7ID: &f").append(skillId).append("'\n");
            yaml.append("      - '&7Cooldown: &f").append(skill.getCooldown()).append("s'\n");
            yaml.append("      - '&7Mana: &f").append(skill.getManaCost()).append("'\n");
            yaml.append("      - '&7Click to cast'\n");
            yaml.append("    actions:\n");
            yaml.append("      - 'ki:skill_self ").append(skillId).append("'\n");
            yaml.append("      - 'message:&aCasted: ").append(skill.getDisplayName()).append("'\n");
            yaml.append("      - 'particle:SPELL_WITCH'\n\n");
            slotIndex++;
            if (slotIndex >= 26) break;
        }

        saveYaml(dir, "skill_panel.yml", yaml.toString());
    }

    // ==================== RPG 状态面板 ====================
    private void generateRpgStatus(File dir) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# KiEngine RPG Status - Auto-generated\n");
        yaml.append("title: '&6RPG Status - %player_name%'\n");
        yaml.append("rows: 5\n\n");
        yaml.append("layout:\n");
        yaml.append("  - '#########'\n");
        yaml.append("  - '# A B C #'\n");
        yaml.append("  - '# D E F #'\n");
        yaml.append("  - '# G H I #'\n");
        yaml.append("  - '###B#####'\n\n");
        yaml.append("items:\n");
        yaml.append("  '#':\n    material: BLACK_STAINED_GLASS_PANE\n    name: ' '&n\n\n");
        yaml.append("  'B':\n    material: BARRIER\n    name: '&cBack'\n    actions:\n      - 'menu:kiengine/main_menu'\n\n");
        yaml.append("  'A':\n    material: IRON_SWORD\n    name: '&eCombat Level'\n    lore:\n      - '&7Level: &f%ki_combat_level%'\n      - '&7Exp: &f%ki_combat_exp%'\n\n");
        yaml.append("  'B':\n    material: FISHING_ROD\n    name: '&eFishing Level'\n    lore:\n      - '&7Level: &f%ki_fishing_level%'\n      - '&7Exp: &f%ki_fishing_exp%'\n\n");
        yaml.append("  'C':\n    material: WHEAT\n    name: '&eFarming Level'\n    lore:\n      - '&7Level: &f%ki_farming_level%'\n      - '&7Exp: &f%ki_farming_exp%'\n\n");
        yaml.append("  'D':\n    material: POTION\n    name: '&eAlchemy Level'\n    lore:\n      - '&7Level: &f%ki_alchemy_level%'\n      - '&7Exp: &f%ki_alchemy_exp%'\n\n");
        yaml.append("  'E':\n    material: ANVIL\n    name: '&eSmithing Level'\n    lore:\n      - '&7Level: &f%ki_smithing_level%'\n      - '&7Exp: &f%ki_smithing_exp%'\n\n");
        yaml.append("  'F':\n    material: BOOK\n    name: '&eMagic Level'\n    lore:\n      - '&7Level: &f%ki_magic_level%'\n      - '&7Exp: &f%ki_magic_exp%'\n\n");
        yaml.append("  'G':\n    material: LEATHER_BOOTS\n    name: '&eExploration Level'\n    lore:\n      - '&7Level: &f%ki_exploration_level%'\n      - '&7Exp: &f%ki_exploration_exp%'\n\n");
        yaml.append("  'H':\n    material: GOLD_INGOT\n    name: '&eTotal Stats'\n    lore:\n      - '&7Total Level: &f%ki_total_level%'\n      - '&7Total Exp: &f%ki_total_exp%'\n\n");
        yaml.append("  'I':\n    material: EMERALD\n    name: '&eLeaderboard'\n    lore:\n      - '&7Click to view rankings'\n    actions:\n      - 'message:&7Leaderboard coming soon!'\n\n");

        saveYaml(dir, "rpg_status.yml", yaml.toString());
    }

    // ==================== 烹饪锅界面 ====================
    private void generateCookingPot(File dir) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# KiEngine Cooking Pot - Auto-generated\n");
        yaml.append("title: '&6Cooking Pot'\n");
        yaml.append("rows: 5\n\n");
        yaml.append("layout:\n");
        yaml.append("  - '#########'\n");
        yaml.append("  - '#i i i  #'\n");
        yaml.append("  - '#i i i F#'\n");
        yaml.append("  - '#  o    #'\n");
        yaml.append("  - '###B#####'\n\n");
        yaml.append("items:\n");
        yaml.append("  '#':\n    material: BLACK_STAINED_GLASS_PANE\n    name: ' '&n\n\n");
        yaml.append("  'i':\n    material: WHITE_STAINED_GLASS_PANE\n    name: '&7Ingredient Slot'\n    lore:\n      - '&7Place ingredients here'\n\n");
        yaml.append("  'F':\n    material: CAMPFIRE\n    name: '&6Fire'\n    lore:\n      - '&7Cooking progress'\n      - '&7Status: &aActive'\n\n");
        yaml.append("  'o':\n    material: BOWL\n    name: '&eOutput'\n    lore:\n      - '&7Result will appear here'\n\n");
        yaml.append("  'B':\n    material: BARRIER\n    name: '&cClose'\n    actions:\n      - 'close'\n\n");

        saveYaml(dir, "cooking_pot.yml", yaml.toString());
    }

    // ==================== NPC 对话模板 ====================
    private void generateNpcDialog(File dir) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# KiEngine NPC Dialog Template - Auto-generated\n");
        yaml.append("# This is a template. NPC dialogs should be created per-NPC.\n");
        yaml.append("title: '&6NPC Dialog'\n");
        yaml.append("rows: 6\n\n");
        yaml.append("layout:\n");
        yaml.append("  - '#########'\n");
        yaml.append("  - '#       #'\n");
        yaml.append("  - '#   N   #'\n");
        yaml.append("  - '#       #'\n");
        yaml.append("  - '#A  B  C#'\n");
        yaml.append("  - '#########'\n\n");
        yaml.append("items:\n");
        yaml.append("  '#':\n    material: BLACK_STAINED_GLASS_PANE\n    name: ' '&n\n\n");
        yaml.append("  'N':\n    material: PLAYER_HEAD\n    name: '&eNPC Name'\n    lore:\n      - '&7Hello, adventurer!'\n      - '&7What can I do for you?'\n\n");
        yaml.append("  'A':\n    material: EMERALD\n    name: '&aTrade'\n    lore:\n      - '&7Open trading interface'\n    actions:\n      - 'message:&7Trade system coming soon!'\n\n");
        yaml.append("  'B':\n    material: BOOK\n    name: '&eQuest'\n    lore:\n      - '&7View available quests'\n    actions:\n      - 'message:&7Quest system coming soon!'\n\n");
        yaml.append("  'C':\n    material: BARRIER\n    name: '&cLeave'\n    actions:\n      - 'close'\n\n");

        saveYaml(dir, "npc_dialog.yml", yaml.toString());
    }

    private void saveYaml(File dir, String filename, String content) {
        File file = new File(dir, filename);
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            plugin.getLogger().warning("[KiMenu] Failed to write " + filename + ": " + e.getMessage());
        }
    }
}
