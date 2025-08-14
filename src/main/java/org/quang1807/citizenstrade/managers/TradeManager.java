package org.quang1807.citizenstrade.managers;

import org.quang1807.citizenstrade.CitizenShop;
import org.quang1807.citizenstrade.models.TradeData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeManager {
    private final CitizenShop plugin;
    private final EconomyManager economyManager;

    public TradeManager(CitizenShop plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
    }

    public boolean canAffordTrade(Player player, TradeData trade) {
        return canAffordTrade(player, trade, 1);
    }

    public boolean canAffordTrade(Player player, TradeData trade, int times) {
        // Check money requirement
        if (trade.getRequiredMoney() > 0) {
            if (!economyManager.hasEnoughMoney(player, trade.getRequiredMoney() * times)) {
                return false;
            }
        }

        // Check points requirement
        if (trade.getRequiredPoints() > 0) {
            if (!economyManager.hasEnoughPoints(player, trade.getRequiredPoints() * times)) {
                return false;
            }
        }

        // Check item requirements
        if (trade.getRequiredItems() != null && !trade.getRequiredItems().isEmpty()) {
            return hasRequiredItems(player, trade.getRequiredItems(), times);
        }

        return true;
    }

    public int calculateMaxTrades(Player player, TradeData trade) {
        int maxTrades = Integer.MAX_VALUE;

        // Check money limit
        if (trade.getRequiredMoney() > 0) {
            double balance = economyManager.getBalance(player);
            if (balance < trade.getRequiredMoney()) return 0;
            maxTrades = Math.min(maxTrades, (int) (balance / trade.getRequiredMoney()));
        }

        // Check points limit
        if (trade.getRequiredPoints() > 0) {
            int points = economyManager.getPoints(player);
            if (points < trade.getRequiredPoints()) return 0;
            maxTrades = Math.min(maxTrades, points / trade.getRequiredPoints());
        }

        // Check items limit
        if (trade.getRequiredItems() != null) {
            for (ItemStack requiredItem : trade.getRequiredItems()) {
                if (requiredItem == null || requiredItem.getType() == Material.AIR) continue;
                int playerAmount = countItems(player, requiredItem);
                if (playerAmount < requiredItem.getAmount()) return 0;
                maxTrades = Math.min(maxTrades, playerAmount / requiredItem.getAmount());
            }
        }

        return maxTrades == Integer.MAX_VALUE ? 1 : maxTrades;
    }

    public boolean executeTrade(Player player, TradeData trade, int times) {
        if (!canAffordTrade(player, trade, times)) {
            return false;
        }

        // Take requirements
        takeRequirements(player, trade, times);

        // Give rewards
        giveRewards(player, trade, times);

        return true;
    }

    private void takeRequirements(Player player, TradeData trade, int times) {
        // Take money
        if (trade.getRequiredMoney() > 0) {
            economyManager.takeMoney(player, trade.getRequiredMoney() * times);
        }

        // Take points
        if (trade.getRequiredPoints() > 0) {
            economyManager.takePoints(player, trade.getRequiredPoints() * times);
        }

        // Take items
        if (trade.getRequiredItems() != null) {
            removeItems(player, trade.getRequiredItems(), times);
        }
    }

    private void giveRewards(Player player, TradeData trade, int times) {
        // Give money
        if (trade.getRewardMoney() > 0) {
            economyManager.giveMoney(player, trade.getRewardMoney() * times);
        }

        // Give points
        if (trade.getRewardPoints() > 0) {
            economyManager.givePoints(player, trade.getRewardPoints() * times);
        }

        // Give items
        if (trade.getRewardItems() != null) {
            for (int i = 0; i < times; i++) {
                for (ItemStack item : trade.getRewardItems()) {
                    if (item != null && item.getType() != Material.AIR) {
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                        if (!leftover.isEmpty()) {
                            for (ItemStack drop : leftover.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), drop);
                            }
                            player.sendMessage(plugin.getConfigManager().getMessage("trade-inventory-full"));
                        }
                    }
                }
            }
        }
    }

    private boolean hasRequiredItems(Player player, List<ItemStack> required, int times) {
        if (required == null || required.isEmpty()) {
            return true;
        }

        for (ItemStack requiredItem : required) {
            if (requiredItem == null || requiredItem.getType() == Material.AIR) continue;
            int totalRequiredAmount = requiredItem.getAmount() * times;
            if (countItems(player, requiredItem) < totalRequiredAmount) {
                return false;
            }
        }
        return true;
    }

    private void removeItems(Player player, List<ItemStack> toRemove, int times) {
        if (toRemove == null || toRemove.isEmpty() || times <= 0) {
            return;
        }

        for (ItemStack itemToRemove : toRemove) {
            if (itemToRemove != null && itemToRemove.getType() != Material.AIR) {
                ItemStack totalToRemove = itemToRemove.clone();
                totalToRemove.setAmount(itemToRemove.getAmount() * times);
                player.getInventory().removeItem(totalToRemove);
            }
        }
        player.updateInventory();
    }

    public int countItems(Player player, ItemStack item) {
        if (item == null) {
            return 0;
        }

        int count = 0;
        for (ItemStack playerItem : player.getInventory().getContents()) {
            if (playerItem != null && playerItem.isSimilar(item)) {
                count += playerItem.getAmount();
            }
        }
        return count;
    }
}