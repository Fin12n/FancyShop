package org.quang1807.citizenstrade.config;

import org.quang1807.citizenstrade.CitizenShop;
import org.quang1807.citizenstrade.models.TradeData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeDataManager {
    private final CitizenShop plugin;
    private File tradesFile;
    private FileConfiguration tradesConfig;

    public TradeDataManager(CitizenShop plugin) {
        this.plugin = plugin;
        setupFiles();
    }

    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        tradesFile = new File(plugin.getDataFolder(), "trades.yml");
        if (!tradesFile.exists()) {
            try {
                tradesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create trades.yml: " + e.getMessage());
            }
        }

        tradesConfig = YamlConfiguration.loadConfiguration(tradesFile);
    }

    public void loadTrades() {
        plugin.getNpcTrades().clear();

        if (!tradesConfig.contains("trades")) {
            return;
        }

        ConfigurationSection tradesSection = tradesConfig.getConfigurationSection("trades");
        if (tradesSection == null) {
            return;
        }

        for (String npcId : tradesSection.getKeys(false)) {
            ConfigurationSection npcSection = tradesSection.getConfigurationSection(npcId);
            if (npcSection == null) continue;

            Map<String, TradeData> npcTradeMap = new HashMap<>();

            for (String tradeId : npcSection.getKeys(false)) {
                ConfigurationSection tradeSection = npcSection.getConfigurationSection(tradeId);
                if (tradeSection == null) continue;

                TradeData trade = new TradeData();
                trade.setRequiredMoney(tradeSection.getDouble("requiredMoney", 0));
                trade.setRequiredPoints(tradeSection.getInt("requiredPoints", 0));
                trade.setRewardMoney(tradeSection.getDouble("rewardMoney", 0));
                trade.setRewardPoints(tradeSection.getInt("rewardPoints", 0));

                if (tradeSection.contains("requiredItems")) {
                    @SuppressWarnings("unchecked")
                    List<ItemStack> requiredItems = (List<ItemStack>) tradeSection.getList("requiredItems");
                    trade.setRequiredItems(requiredItems);
                }

                if (tradeSection.contains("rewardItems")) {
                    @SuppressWarnings("unchecked")
                    List<ItemStack> rewardItems = (List<ItemStack>) tradeSection.getList("rewardItems");
                    trade.setRewardItems(rewardItems);
                }

                npcTradeMap.put(tradeId, trade);
            }

            if (!npcTradeMap.isEmpty()) {
                plugin.getNpcTrades().put(npcId, npcTradeMap);
            }
        }

        plugin.getLogger().info("Loaded trades for " + plugin.getNpcTrades().size() + " NPCs");
    }

    public void saveTrades() {
        tradesConfig.set("trades", null);

        for (Map.Entry<String, Map<String, TradeData>> npcEntry : plugin.getNpcTrades().entrySet()) {
            String npcId = npcEntry.getKey();
            Map<String, TradeData> npcTradeMap = npcEntry.getValue();

            for (Map.Entry<String, TradeData> tradeEntry : npcTradeMap.entrySet()) {
                String tradeId = tradeEntry.getKey();
                TradeData trade = tradeEntry.getValue();

                String path = "trades." + npcId + "." + tradeId;
                tradesConfig.set(path + ".requiredMoney", trade.getRequiredMoney());
                tradesConfig.set(path + ".requiredPoints", trade.getRequiredPoints());
                tradesConfig.set(path + ".rewardMoney", trade.getRewardMoney());
                tradesConfig.set(path + ".rewardPoints", trade.getRewardPoints());

                if (trade.getRequiredItems() != null && !trade.getRequiredItems().isEmpty()) {
                    tradesConfig.set(path + ".requiredItems", trade.getRequiredItems());
                }

                if (trade.getRewardItems() != null && !trade.getRewardItems().isEmpty()) {
                    tradesConfig.set(path + ".rewardItems", trade.getRewardItems());
                }
            }
        }

        try {
            tradesConfig.save(tradesFile);
            plugin.getLogger().info("Trades saved successfully!");
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving trades: " + e.getMessage());
        }
    }

    public FileConfiguration getTradesConfig() {
        return tradesConfig;
    }
}