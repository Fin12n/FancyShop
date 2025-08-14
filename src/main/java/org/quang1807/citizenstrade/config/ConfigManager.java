package org.quang1807.citizenstrade.config;

import org.quang1807.citizenstrade.CitizenShop;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final CitizenShop plugin;
    private File configFile;
    private File guiConfigFile;
    private FileConfiguration config;
    private FileConfiguration guiConfig;

    public ConfigManager(CitizenShop plugin) {
        this.plugin = plugin;
        setupFiles();
    }

    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        guiConfigFile = new File(plugin.getDataFolder(), "gui.yml");

        // Save default configurations if they don't exist
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        if (!guiConfigFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
    }

    public void loadConfigs() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            guiConfig = YamlConfiguration.loadConfiguration(guiConfigFile);
            plugin.getLogger().info("Configuration files loaded successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading configuration files: " + e.getMessage());
        }
    }

    public void saveConfigs() {
        try {
            if (config != null) {
                config.save(configFile);
            }
            if (guiConfig != null) {
                guiConfig.save(guiConfigFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving configuration files: " + e.getMessage());
        }
    }

    public String getMessage(String key) {
        String message = config.getString("messages." + key, "&cMessage not found: " + key);
        return translateColors(message);
    }

    public String translateColors(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // Getters
    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public File getConfigFile() {
        return configFile;
    }

    public File getGuiConfigFile() {
        return guiConfigFile;
    }
}