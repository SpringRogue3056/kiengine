package com.ki.engine.core;

import com.ki.engine.addon.AddonManager;
import com.ki.engine.addon.AddonRegistry;
import com.ki.engine.api.KiEngineAPI;
import com.ki.engine.block.BlockManager;
import com.ki.engine.block.BlockManagerImpl;
import com.ki.engine.block.noteblock.NoteBlockListener;
import com.ki.engine.block.noteblock.NoteBlockManager;
import com.ki.engine.command.KiCommand;
import com.ki.engine.config.ConfigManager;
import com.ki.engine.database.DatabaseManager;
import com.ki.engine.enchantment.EnchantmentListener;
import com.ki.engine.enchantment.EnchantmentManager;
import com.ki.engine.entity.EntityManager;
import com.ki.engine.entity.EntityManagerImpl;
import com.ki.engine.event.KiEngineLoadEvent;
import com.ki.engine.farmersdelight.FarmersDelightManager;
import com.ki.engine.crop.CropManager;
import com.ki.engine.gui.GuiManager;
import com.ki.engine.gui.impl.SimpleGuiListener;
import com.ki.engine.item.ItemManager;
import com.ki.engine.item.ItemManagerImpl;
import com.ki.engine.listener.BlockInteractListener;
import com.ki.engine.listener.CookingPotListener;
import com.ki.engine.listener.CropListener;
import com.ki.engine.listener.CuttingBoardListener;
import com.ki.engine.listener.FishingListener;
import com.ki.engine.listener.GuiClickListener;
import com.ki.engine.listener.ItemUseListener;
import com.ki.engine.npc.NpcManager;
import com.ki.engine.npc.NpcManagerImpl;
import com.ki.engine.placeholder.KiEngineExpansion;
import com.ki.engine.recipe.RecipeManager;
import com.ki.engine.recipe.RecipeManagerImpl;
import com.ki.engine.resourcepack.ResourcePackManager;
import com.ki.engine.rpg.RPGManager;
import com.ki.engine.skill.SkillManager;
import com.ki.engine.skill.SkillManagerImpl;
import com.ki.engine.survival.FoodListener;
import com.ki.engine.survival.MobSkillListener;
import com.ki.engine.survival.RPGAttributeListener;
import com.ki.engine.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * KiEngine - Minecraft content engine with lifecycle management.
 *
 * Lifecycle phases:
 *   1. PRE_INIT   - Create ConfigManager, load raw configs
 *   2. INIT       - Create and init all Managers (order-resolved)
 *   3. POST_INIT  - Register listeners, commands, bridges, fire events
 */
public class KiEnginePlugin extends JavaPlugin {

    private static KiEnginePlugin instance; // Legacy access, prefer KiEngineAPI

    // Core
    private ConfigManager configManager;
    private Scheduler scheduler;

    // Managers (initialized via lifecycle)
    private final List<Manager> managers = new ArrayList<>();
    private ItemManager itemManager;
    private BlockManager blockManager;
    private EntityManager entityManager;
    private RecipeManager recipeManager;
    private SkillManager skillManager;
    private NpcManager npcManager;

    // Feature modules
    private FarmersDelightManager farmersDelightManager;
    private RPGManager rpgManager;
    private GuiManager guiManager;
    private EnchantmentManager enchantmentManager;
    private AddonManager addonManager;
    private CropManager cropManager;
    private DatabaseManager databaseManager;
    private ResourcePackManager resourcePackManager;
    private NoteBlockManager noteBlockManager;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();
        getLogger().info("========================================");
        getLogger().info("  KiEngine v" + getDescription().getVersion());
        getLogger().info("  Loading...");
        getLogger().info("========================================");

        try {
            phasePreInit();
            phaseInit();
            phasePostInit();
        } catch (Exception e) {
            getLogger().severe("[KiEngine] CRITICAL ERROR during startup: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        long elapsed = System.currentTimeMillis() - start;
        getLogger().info("KiEngine loaded in " + elapsed + "ms");
        getLogger().info("Items: " + itemManager.getRegistry().size()
                + " | Recipes: " + recipeManager.getRegistry().size()
                + " | Mobs: " + entityManager.getMobRegistry().size()
                + " | Skills: " + skillManager.getRegistry().size());
    }

    // ========== Lifecycle Phases ==========

    private void phasePreInit() {
        this.configManager = new ConfigManager(this);
        this.scheduler = new Scheduler(this);
        configManager.loadAll();
    }

    private void phaseInit() {
        // Create managers in dependency order
        this.itemManager = new ItemManagerImpl(this);
        managers.add((com.ki.engine.core.Manager) itemManager);
        
        this.blockManager = new BlockManagerImpl(this);
        managers.add((com.ki.engine.core.Manager) blockManager);
        
        this.recipeManager = new RecipeManagerImpl(this);
        managers.add((com.ki.engine.core.Manager) recipeManager);
        
        this.entityManager = new EntityManagerImpl(this);
        managers.add((com.ki.engine.core.Manager) entityManager);
        
        this.npcManager = new NpcManagerImpl(this);
        managers.add((com.ki.engine.core.Manager) npcManager);
        
        this.skillManager = new SkillManagerImpl(this);
        managers.add((com.ki.engine.core.Manager) skillManager);

        this.farmersDelightManager = new FarmersDelightManager(this);
        this.rpgManager = new RPGManager(this);
        this.guiManager = new GuiManager(this);
        this.enchantmentManager = new EnchantmentManager(this);
        this.cropManager = new CropManager(this);
        this.databaseManager = new DatabaseManager(this);
        managers.add((com.ki.engine.core.Manager) databaseManager);
        this.resourcePackManager = new ResourcePackManager(this);
        managers.add((com.ki.engine.core.Manager) resourcePackManager);
        this.noteBlockManager = new NoteBlockManager(this);
        managers.add((com.ki.engine.core.Manager) noteBlockManager);

        // Resolve cross-manager dependencies
        ((RecipeManagerImpl) recipeManager).setItemManager(itemManager);

        // Initialize all managers
        for (com.ki.engine.core.Manager m : managers) {
            m.init();
        }
    }

    private void phasePostInit() {
        // Register Bukkit listeners
        Bukkit.getPluginManager().registerEvents(new CookingPotListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CuttingBoardListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CropListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FishingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemUseListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SimpleGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new NoteBlockListener(this), this);

        // Enchantment system
        EnchantmentListener enchantmentListener = new EnchantmentListener(this, enchantmentManager);
        Bukkit.getPluginManager().registerEvents(enchantmentListener, this);

        // Survival experience listeners
        Bukkit.getPluginManager().registerEvents(new FoodListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobSkillListener(this), this);
        Bukkit.getPluginManager().registerEvents(new RPGAttributeListener(this), this);

        // Commands
        KiCommand cmd = new KiCommand(this);
        getCommand("ki").setExecutor(cmd);
        getCommand("ki").setTabCompleter(cmd);

        // Public API
        KiEngineAPI.init(this);

        // Load addons after all managers ready
        this.addonManager = new AddonManager(this);
        addonManager.loadAddons();

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KiEngineExpansion(this).register();
        }

        // Fire load event
        getServer().getPluginManager().callEvent(new KiEngineLoadEvent());
    }

    @Override
    public void onDisable() {
        long start = System.currentTimeMillis();

        // Shutdown in reverse order
        for (int i = managers.size() - 1; i >= 0; i--) {
            try {
                managers.get(i).shutdown();
            } catch (Exception e) {
                getLogger().warning("Error shutting down manager: " + e.getMessage());
            }
        }

        if (guiManager != null) guiManager.getMenuBridge().unregister();
        if (scheduler != null) scheduler.cancelAll();
        if (rpgManager != null) rpgManager.save();
        if (addonManager != null) addonManager.shutdown();
        if (configManager != null) configManager.shutdown();

        long elapsed = System.currentTimeMillis() - start;
        getLogger().info("KiEngine disabled in " + elapsed + "ms");
    }

    // ========== Getters ==========

    public static KiEnginePlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public Scheduler getScheduler() { return scheduler; }
    public ItemManager getItemManager() { return itemManager; }
    public BlockManager getBlockManager() { return blockManager; }
    public EntityManager getEntityManager() { return entityManager; }
    public RecipeManager getRecipeManager() { return recipeManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public NpcManager getNpcManager() { return npcManager; }
    public FarmersDelightManager getFarmersDelightManager() { return farmersDelightManager; }
    public RPGManager getRpgManager() { return rpgManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public EnchantmentManager getEnchantmentManager() { return enchantmentManager; }
    public AddonManager getAddonManager() { return addonManager; }
    public AddonRegistry getAddonRegistry() { return KiEngineAPI.getAddonRegistry(); }
    public CropManager getCropManager() { return cropManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ResourcePackManager getResourcePackManager() { return resourcePackManager; }
    public NoteBlockManager getNoteBlockManager() { return noteBlockManager; }
}
