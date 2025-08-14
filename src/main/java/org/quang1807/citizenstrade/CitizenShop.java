package org.quang1807.citizenstrade;

import org.quang1807.citizenstrade.commands.CommandManager;
import org.quang1807.citizenstrade.config.ConfigManager;
import org.quang1807.citizenstrade.config.TradeDataManager;
import org.quang1807.citizenstrade.gui.GuiManager;
import org.quang1807.citizenstrade.listeners.ChatListener;
import org.quang1807.citizenstrade.listeners.InventoryListener;
import org.quang1807.citizenstrade.listeners.NPCClickListener;
import org.quang1807.citizenstrade.managers.EconomyManager;
import org.quang1807.citizenstrade.managers.TradeManager;
import org.quang1807.citizenstrade.models.TradeData;
import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CitizenShop extends JavaPlugin {

    private static CitizenShop instance;

    // Managers
    private ConfigManager configManager;
    private TradeDataManager tradeDataManager;
    private EconomyManager economyManager;
    private TradeManager tradeManager;
    private GuiManager guiManager;

    // Player states
    private Map<UUID, String> editingNpc = new HashMap<>();
    private Map<UUID, String> editingTradeId = new HashMap<>();
    private Map<UUID, String> chatInput = new HashMap<>();
    private Map<UUID, String> currentTradeNpc = new HashMap<>();
    private Map<UUID, String> currentTradeId = new HashMap<>();
    private Map<UUID, Integer> playerCurrentPage = new HashMap<>();
    private Map<UUID, Integer> playerShopPage = new HashMap<>();
    private Map<UUID, Map<Integer, String>> playerTradeSlots = new HashMap<>();

    // Cache
    private Map<String, Location> npcLocations = new HashMap<>();
    private Map<String, Map<String, TradeData>> npcTrades = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers FIRST
        configManager = new ConfigManager(this);
        economyManager = new EconomyManager(this); // Initialize BEFORE setupDependencies()
        tradeDataManager = new TradeDataManager(this);
        tradeManager = new TradeManager(this);
        guiManager = new GuiManager(this);

        // Check for required plugins AFTER initializing managers
        if (!setupDependencies()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load configurations and trades
        configManager.loadConfigs();
        tradeDataManager.loadTrades();

        // Register events
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCClickListener(this), this);

        // Register commands
        getCommand("cs").setExecutor(new CommandManager(this));

        // Cache NPC locations after server load
        new BukkitRunnable() {
            @Override
            public void run() {
                cacheNpcLocations();
            }
        }.runTaskLater(this, 40L); // Increase delay for Citizens to fully load

        getLogger().info("CitizenShop has been enabled!");
    }

    @Override
    public void onDisable() {
        if (tradeDataManager != null) {
            tradeDataManager.saveTrades();
        }
        getLogger().info("CitizenShop has been disabled!");
    }

    private boolean setupDependencies() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault not found! Plugin disabled.");
            return false;
        }

        if (getServer().getPluginManager().getPlugin("Citizens") == null) {
            getLogger().severe("Citizens not found! Plugin disabled.");
            return false;
        }

        // Now economyManager should be initialized, so this won't throw NullPointerException
        if (!economyManager.setupEconomy()) {
            getLogger().severe("Economy system not found! Plugin disabled.");
            return false;
        }

        if (!economyManager.setupPlayerPoints()) {
            getLogger().warning("PlayerPoints not found! Point trading will be disabled.");
        }

        return true;
    }

    public void cacheNpcLocations() {
        npcLocations.clear();
        try {
            NPCRegistry registry = CitizensAPI.getNPCRegistry();
            if (registry != null) {
                for (NPC npc : registry) {
                    if (npc != null && npc.isSpawned() && npc.getEntity() != null) {
                        String npcId = String.valueOf(npc.getId());
                        npcLocations.put(npcId, npc.getEntity().getLocation());
                    }
                }
                getLogger().info("Cached " + npcLocations.size() + " NPC locations");
            }
        } catch (Exception e) {
            getLogger().warning("Could not cache NPC locations: " + e.getMessage());
        }
    }

    public void reload() {
        configManager.loadConfigs();
        tradeDataManager.loadTrades();
        cacheNpcLocations();
    }

    // Getters
    public static CitizenShop getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TradeDataManager getTradeDataManager() {
        return tradeDataManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    // Player states getters/setters
    public Map<UUID, String> getEditingNpc() {
        return editingNpc;
    }

    public Map<UUID, String> getEditingTradeId() {
        return editingTradeId;
    }

    public Map<UUID, String> getChatInput() {
        return chatInput;
    }

    public Map<UUID, String> getCurrentTradeNpc() {
        return currentTradeNpc;
    }

    public Map<UUID, String> getCurrentTradeId() {
        return currentTradeId;
    }

    public Map<UUID, Integer> getPlayerCurrentPage() {
        return playerCurrentPage;
    }

    public Map<UUID, Integer> getPlayerShopPage() {
        return playerShopPage;
    }

    public Map<UUID, Map<Integer, String>> getPlayerTradeSlots() {
        return playerTradeSlots;
    }

    public Map<String, Location> getNpcLocations() {
        return npcLocations;
    }

    public Map<String, Map<String, TradeData>> getNpcTrades() {
        return npcTrades;
    }
}