package org.quang1807.fancyshop.managers

import org.quang1807.fancyshop.FancyShop
import org.quang1807.fancyshop.data.TradeData
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import java.util.*

class TradeManager(private val plugin: FancyShop) {

    private val npcTrades = mutableMapOf<String, MutableMap<String, TradeData>>()

    fun loadTrades() {
        npcTrades.clear()
        val tradesConfig = plugin.configManager.tradesConfig

        if (!tradesConfig.contains("trades")) return

        val tradesSection = tradesConfig.getConfigurationSection("trades") ?: return

        for (npcId in tradesSection.getKeys(false)) {
            val npcSection = tradesSection.getConfigurationSection(npcId) ?: continue
            val npcTradeMap = mutableMapOf<String, TradeData>()

            for (tradeId in npcSection.getKeys(false)) {
                val tradeSection = npcSection.getConfigurationSection(tradeId) ?: continue

                val trade = TradeData().apply {
                    requiredMoney = tradeSection.getDouble("requiredMoney", 0.0)
                    requiredPoints = tradeSection.getInt("requiredPoints", 0)
                    rewardMoney = tradeSection.getDouble("rewardMoney", 0.0)
                    rewardPoints = tradeSection.getInt("rewardPoints", 0)

                    if (tradeSection.contains("requiredItems")) {
                        requiredItems = (tradeSection.getList("requiredItems") as? List<ItemStack>)?.toMutableList() ?: mutableListOf()
                    }
                    if (tradeSection.contains("rewardItems")) {
                        rewardItems = (tradeSection.getList("rewardItems") as? List<ItemStack>)?.toMutableList() ?: mutableListOf()
                    }
                }

                npcTradeMap[tradeId] = trade
            }

            if (npcTradeMap.isNotEmpty()) {
                npcTrades[npcId] = npcTradeMap
            }
        }

        plugin.logger.info("Loaded trades for ${npcTrades.size} NPCs")
    }

    fun saveTrades() {
        val tradesConfig = plugin.configManager.tradesConfig
        tradesConfig.set("trades", null)

        npcTrades.forEach { (npcId, npcTradeMap) ->
            npcTradeMap.forEach { (tradeId, trade) ->
                val path = "trades.$npcId.$tradeId"
                tradesConfig.set("$path.requiredMoney", trade.requiredMoney)
                tradesConfig.set("$path.requiredPoints", trade.requiredPoints)
                tradesConfig.set("$path.rewardMoney", trade.rewardMoney)
                tradesConfig.set("$path.rewardPoints", trade.rewardPoints)

                if (trade.requiredItems.isNotEmpty()) {
                    tradesConfig.set("$path.requiredItems", trade.requiredItems)
                }
                if (trade.rewardItems.isNotEmpty()) {
                    tradesConfig.set("$path.rewardItems", trade.rewardItems)
                }
            }
        }

        plugin.configManager.saveTradesConfig()
    }

    fun getNPCTrades(npcId: String): Map<String, TradeData>? {
        return npcTrades[npcId]
    }

    fun getTrade(npcId: String, tradeId: String): TradeData? {
        return npcTrades[npcId]?.get(tradeId)
    }

    fun addTrade(npcId: String, tradeId: String, trade: TradeData) {
        npcTrades.computeIfAbsent(npcId) { mutableMapOf() }[tradeId] = trade
    }

    fun removeTrade(npcId: String, tradeId: String) {
        npcTrades[npcId]?.remove(tradeId)
        if (npcTrades[npcId]?.isEmpty() == true) {
            npcTrades.remove(npcId)
        }
    }

    fun getAllNPCTrades(): Map<String, Map<String, TradeData>> {
        return npcTrades.toMap()
    }
}