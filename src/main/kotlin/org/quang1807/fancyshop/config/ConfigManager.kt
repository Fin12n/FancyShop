package org.quang1807.fancyshop.config

import org.quang1807.fancyshop.FancyShop
import org.bukkit.ChatColor
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

class ConfigManager(private val plugin: FancyShop) {

    private lateinit var configFile: File
    private lateinit var guiConfigFile: File
    private lateinit var tradesFile: File

    lateinit var config: FileConfiguration
        private set
    lateinit var guiConfig: FileConfiguration
        private set
    lateinit var tradesConfig: FileConfiguration
        private set

    fun loadConfigs() {
        setupFiles()
        config = YamlConfiguration.loadConfiguration(configFile)
        guiConfig = YamlConfiguration.loadConfiguration(guiConfigFile)
        tradesConfig = YamlConfiguration.loadConfiguration(tradesFile)
    }

    private fun setupFiles() {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        configFile = File(plugin.dataFolder, "config.yml")
        guiConfigFile = File(plugin.dataFolder, "gui.yml")
        tradesFile = File(plugin.dataFolder, "trades.yml")

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }

        if (!guiConfigFile.exists()) {
            plugin.saveResource("gui.yml", false)
        }

        if (!tradesFile.exists()) {
            try {
                tradesFile.createNewFile()
            } catch (e: IOException) {
                plugin.logger.severe("Could not create trades.yml: ${e.message}")
            }
        }
    }

    fun getMessage(key: String): String {
        return translateColors(config.getString("messages.$key", "&cMessage not found: $key") ?: "&cMessage not found: $key")
    }

    fun translateColors(text: String): String {
        return ChatColor.translateAlternateColorCodes('&', text)
    }

    fun saveTradesConfig() {
        try {
            tradesConfig.save(tradesFile)
        } catch (e: IOException) {
            plugin.logger.severe("Could not save trades.yml: ${e.message}")
        }
    }
}