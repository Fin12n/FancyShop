package org.quang1807.citizenstrade.managers;

import org.quang1807.citizenstrade.CitizenShop;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private final CitizenShop plugin;
    private Economy economy;
    private PlayerPointsAPI playerPointsAPI;

    public EconomyManager(CitizenShop plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean setupPlayerPoints() {
        if (plugin.getServer().getPluginManager().getPlugin("PlayerPoints") == null) {
            return false;
        }

        try {
            playerPointsAPI = PlayerPoints.getInstance().getAPI();
            return playerPointsAPI != null;
        } catch (Exception e) {
            plugin.getLogger().warning("Error setting up PlayerPoints: " + e.getMessage());
            return false;
        }
    }

    // Economy methods
    public boolean hasEnoughMoney(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public void takeMoney(Player player, double amount) {
        if (economy != null) {
            economy.withdrawPlayer(player, amount);
        }
    }

    public void giveMoney(Player player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }

    public double getBalance(Player player) {
        return economy != null ? economy.getBalance(player) : 0.0;
    }

    // PlayerPoints methods
    public boolean hasEnoughPoints(Player player, int amount) {
        return playerPointsAPI != null && playerPointsAPI.look(player.getUniqueId()) >= amount;
    }

    public void takePoints(Player player, int amount) {
        if (playerPointsAPI != null) {
            playerPointsAPI.take(player.getUniqueId(), amount);
        }
    }

    public void givePoints(Player player, int amount) {
        if (playerPointsAPI != null) {
            playerPointsAPI.give(player.getUniqueId(), amount);
        }
    }

    public int getPoints(Player player) {
        return playerPointsAPI != null ? playerPointsAPI.look(player.getUniqueId()) : 0;
    }

    // Getters
    public Economy getEconomy() {
        return economy;
    }

    public PlayerPointsAPI getPlayerPointsAPI() {
        return playerPointsAPI;
    }

    public boolean isEconomyEnabled() {
        return economy != null;
    }

    public boolean isPlayerPointsEnabled() {
        return playerPointsAPI != null;
    }
}