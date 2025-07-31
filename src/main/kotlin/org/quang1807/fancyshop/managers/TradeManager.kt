package org.quang1807.fancyshop.managers

import org.quang1807.fancyshop.objects.Trade

class TradeManager {

    val npcTrades: MutableMap<String, MutableList<Trade>> = mutableMapOf()

    fun getNPCTrades(npcId: String): List<Trade>? {
        return npcTrades[npcId]
    }

    fun getTradeBySlot(npcId: String, slot: Int): Trade? {
        val trades = npcTrades[npcId] ?: return null
        return trades.firstOrNull { it.slot == slot }
    }

    fun saveTrade(npcId: String, trade: Trade) {
        npcTrades.computeIfAbsent(npcId) { mutableListOf() }.add(trade)
    }
}
