package org.quang1807.fancyshop

import org.bukkit.plugin.java.JavaPlugin
import org.quang1807.fancyshop.commands.FancyShopCommand
import org.quang1807.fancyshop.config.ConfigManager
import org.quang1807.fancyshop.listeners.*
import org.quang1807.fancyshop.managers.*

class FancyShop : JavaPlugin() {

    lateinit var configManager: ConfigManager
        private set
    lateinit var economyManager: EconomyManager
        private set
    lateinit var tradeManager: TradeManager
        private set
    lateinit var npcManager: NPCManager
        private set
    lateinit var guiManager: GUIManager
        private set

    override fun onEnable() {
        // Initialize managers
        configManager = ConfigManager(this)
        economyManager = EconomyManager(this)
        tradeManager = TradeManager(this)
        npcManager = NPCManager(this)
        guiManager = GUIManager(this)

        // Setup economy
        if (!economyManager.setupEconomy()) {
            logger.severe("Vault not found! Plugin disabled.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Setup optional dependencies
        economyManager.setupPlayerPoints()
        npcManager.setupNPCPlugins()

        // Load configurations
        configManager.loadConfigs()
        tradeManager.loadTrades()

        // Register events
        registerEvents()

        // Register commands
        getCommand("fs")?.setExecutor(FancyShopCommand(this))
        getCommand("fancyshop")?.setExecutor(FancyShopCommand(this))

        // Cache NPC locations after 1 second
        server.scheduler.runTaskLater(this, Runnable {
            npcManager.cacheNpcLocations()
        }, 20L)

        logger.info("FancyShop Kotlin Edition has been enabled!")
    }

    override fun onDisable() {
        if (this::configManager.isInitialized && configManager.tradesConfig != null) {
            tradeManager.saveTrades()
        }
        logger.info("FancyShop Kotlin Edition has been disabled!")
    }

    private fun registerEvents() {
        val pm = server.pluginManager
        pm.registerEvents(NPCClickListener(this), this)
        pm.registerEvents(GUIClickListener(this), this)
        pm.registerEvents(ChatListener(this), this)
        pm.registerEvents(InventoryListener(this), this)
    }
}
