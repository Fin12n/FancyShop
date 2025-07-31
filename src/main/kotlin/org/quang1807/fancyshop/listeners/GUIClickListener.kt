package org.quang1807.fancyshop.listeners

import org.quang1807.fancyshop.FancyShop
import org.quang1807.fancyshop.data.TradeData
import org.quang1807.fancyshop.utils.ItemUtils
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class GUIClickListener(private val plugin: FancyShop) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = getInventoryTitle(event)

        when {
            isNpcSelectionGUI(title) -> handleNpcSelectionClick(event, player)
            isNpcShopGUI(title) -> handleNpcShopClick(event, player)
            isTradeManagementGUI(title) -> handleTradeManagementClick(event, player)
            isTradeEditGUI(title) -> handleTradeEditClick(event, player)
        }
    }

    private fun isNpcSelectionGUI(title: String): Boolean {
        val expectedTitle = plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("npc-selection.title", "&9Chọn NPC để chỉnh sửa") ?: "&9Chọn NPC để chỉnh sửa"
        )
        return title == expectedTitle
    }

    private fun isNpcShopGUI(title: String): Boolean {
        val expectedTitle = plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("npc-shop.title", "&1&lTrao Đổi") ?: "&1&lTrao Đổi"
        )
        return title == expectedTitle
    }

    private fun isTradeManagementGUI(title: String): Boolean {
        val titleBase = plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("npc-trade-management.title", "&6&lQuản lý: {npc_id}") ?: "&6&lQuản lý: {npc_id}"
        ).split("{")[0]
        return title.startsWith(titleBase)
    }

    private fun isTradeEditGUI(title: String): Boolean {
        val expectedTitle = plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("trade-edit.title", "&2Chỉnh sửa giao dịch") ?: "&2Chỉnh sửa giao dịch"
        )
        return title.startsWith(expectedTitle.split(" -")[0])
    }

    private fun handleNpcSelectionClick(event: InventoryClickEvent, player: Player) {
        event.isCancelled = true
        plugin.guiManager.handleNpcSelectionClick(player, event.slot, event.currentItem)
    }

    private fun handleNpcShopClick(event: InventoryClickEvent, player: Player) {
        event.isCancelled = true
        val clickedSlot = event.slot
        val npcId = plugin.guiManager.getCurrentTradeNpc(player) ?: return

        when {
            clickedSlot % 9 == 7 -> {
                val row = clickedSlot / 9
                when (row) {
                    0 -> handlePreviousPage(player, npcId)
                    2 -> handleNextPage(player, npcId)
                    5 -> player.closeInventory()
                }
            }
            clickedSlot % 9 == 5 -> {
                handleTradeClick(player, clickedSlot, event.isShiftClick)
            }
        }
    }

    private fun handlePreviousPage(player: Player, npcId: String) {
        // Implementation handled in GUIManager
        val npcTrades = plugin.tradeManager.getNPCTrades(npcId) ?: return
        plugin.guiManager.openNpcShopGUI(player, npcId) // Simplified, actual page logic in GUIManager
    }

    private fun handleNextPage(player: Player, npcId: String) {
        // Implementation handled in GUIManager
        val npcTrades = plugin.tradeManager.getNPCTrades(npcId) ?: return
        plugin.guiManager.openNpcShopGUI(player, npcId) // Simplified, actual page logic in GUIManager
    }

    private fun handleTradeClick(player: Player, slot: Int, isShiftClick: Boolean) {
        val npcId = plugin.guiManager.getCurrentTradeNpc(player) ?: return
        val tradeSlots = getPlayerTradeSlots(player)
        val tradeId = tradeSlots[slot] ?: return

        executeTrade(player, npcId, tradeId, isShiftClick)
    }

    private fun handleTradeManagementClick(event: InventoryClickEvent, player: Player) {
        event.isCancelled = true
        val clickedSlot = event.slot
        val npcId = plugin.guiManager.getEditingNpc(player) ?: return

        when (clickedSlot) {
            plugin.configManager.guiConfig.getInt("npc-trade-management.back-button.slot", 4) -> {
                plugin.guiManager.openNpcSelectionGUI(player)
            }
            plugin.configManager.guiConfig.getInt("npc-trade-management.add-button.slot", 40) -> {
                plugin.guiManager.openTradeEditGUI(player, npcId, UUID.randomUUID().toString())
            }
            else -> {
                handleTradeItemClick(event, player, npcId)
            }
        }
    }

    private fun handleTradeItemClick(event: InventoryClickEvent, player: Player, npcId: String) {
        val clickedItem = event.currentItem ?: return
        val meta = clickedItem.itemMeta ?: return
        val lore = meta.lore ?: return

        var tradeId: String? = null
        for (line in lore) {
            val cleanLine = ChatColor.stripColor(line) ?: ""
            if (cleanLine.startsWith("Trade ID: ")) {
                tradeId = cleanLine.substring(10)
                break
            }
        }

        if (tradeId != null) {
            if (event.click == ClickType.RIGHT) {
                deleteTrade(player, npcId, tradeId)
            } else {
                plugin.guiManager.openTradeEditGUI(player, npcId, tradeId)
            }
        }
    }

    private fun handleTradeEditClick(event: InventoryClickEvent, player: Player) {
        val slot = event.rawSlot
        if (event.clickedInventory == event.whoClicked.inventory) {
            return // Allow normal inventory interaction
        }
        event.isCancelled = true

        val npcId = plugin.guiManager.getEditingNpc(player) ?: return
        val tradeId = plugin.guiManager.getEditingTradeId(player) ?: return

        when (slot) {
            plugin.configManager.guiConfig.getInt("trade-edit.save-button.slot") -> {
                saveTrade(player, event.clickedInventory ?: return, npcId, tradeId)
            }
            plugin.configManager.guiConfig.getInt("trade-edit.back-button.slot") -> {
                plugin.guiManager.openNpcTradeManagementGUI(player, npcId)
            }
            else -> {
                handleCurrencySlotClick(event, player, slot, npcId, tradeId)
            }
        }
    }

    private fun handleCurrencySlotClick(event: InventoryClickEvent, player: Player, slot: Int, npcId: String, tradeId: String) {
        val inputType = when (slot) {
            plugin.configManager.guiConfig.getInt("trade-edit.required-money-slot") -> "required_money"
            plugin.configManager.guiConfig.getInt("trade-edit.required-points-slot") -> "required_points"
            plugin.configManager.guiConfig.getInt("trade-edit.reward-money-slot") -> "reward_money"
            plugin.configManager.guiConfig.getInt("trade-edit.reward-points-slot") -> "reward_points"
            else -> null
        }

        if (inputType != null) {
            if (event.click == ClickType.RIGHT) {
                // Clear the value
                val trade = plugin.tradeManager.getTrade(npcId, tradeId) ?: return
                when (inputType) {
                    "required_money" -> trade.requiredMoney = 0.0
                    "required_points" -> trade.requiredPoints = 0
                    "reward_money" -> trade.rewardMoney = 0.0
                    "reward_points" -> trade.rewardPoints = 0
                }
                plugin.guiManager.openTradeEditGUI(player, npcId, tradeId)
            } else {
                // Start chat input
                plugin.guiManager.setChatInput(player, inputType)
                player.closeInventory()
                val messageKey = if (inputType.contains("money")) "currency-enter-money-amount" else "currency-enter-points-amount"
                player.sendMessage(plugin.configManager.getMessage(messageKey))
            }
        } else {
            // Handle item slots - allow interaction
            event.isCancelled = false
        }
    }

    private fun executeTrade(player: Player, npcId: String, tradeId: String, isShiftClick: Boolean) {
        val trade = plugin.tradeManager.getTrade(npcId, tradeId) ?: return

        val maxTrades = if (isShiftClick) calculateMaxTrades(player, trade) else 1
        if (maxTrades == 0) {
            player.sendMessage(plugin.configManager.getMessage("trade-insufficient-items"))
            return
        }

        // Check requirements
        if (!ItemUtils.hasRequiredItems(player, trade.requiredItems, maxTrades)) {
            player.sendMessage(plugin.configManager.getMessage("trade-insufficient-items"))
            return
        }

        if (trade.requiredMoney > 0) {
            val totalMoney = trade.requiredMoney * maxTrades
            if (plugin.economyManager.economy?.getBalance(player) ?: 0.0 < totalMoney) {
                player.sendMessage(plugin.configManager.getMessage("trade-insufficient-money")
                    .replace("{amount}", totalMoney.toString()))
                return
            }
        }

        if (trade.requiredPoints > 0) {
            val totalPoints = trade.requiredPoints * maxTrades
            val playerPoints = plugin.economyManager.playerPointsAPI?.look(player.uniqueId) ?: 0
            if (playerPoints < totalPoints) {
                player.sendMessage(plugin.configManager.getMessage("trade-insufficient-points")
                    .replace("{amount}", totalPoints.toString()))
                return
            }
        }

        // Execute trade
        ItemUtils.removeItems(player, trade.requiredItems, maxTrades)

        if (trade.requiredMoney > 0) {
            plugin.economyManager.economy?.withdrawPlayer(player, trade.requiredMoney * maxTrades)
        }

        if (trade.requiredPoints > 0) {
            plugin.economyManager.playerPointsAPI?.take(player.uniqueId, trade.requiredPoints * maxTrades)
        }

        // Give rewards
        if (trade.rewardItems.isNotEmpty()) {
            ItemUtils.giveItems(player, trade.rewardItems, maxTrades)
        }

        if (trade.rewardMoney > 0) {
            plugin.economyManager.economy?.depositPlayer(player, trade.rewardMoney * maxTrades)
        }

        if (trade.rewardPoints > 0) {
            plugin.economyManager.playerPointsAPI?.give(player.uniqueId, trade.rewardPoints * maxTrades)
        }

        // Send success message
        val messageKey = if (isShiftClick && maxTrades > 1) "trade-completed-multiple" else "trade-completed"
        val message = plugin.configManager.getMessage(messageKey)
        if (isShiftClick && maxTrades > 1) {
            player.sendMessage(message.replace("{times}", maxTrades.toString()))
        } else {
            player.sendMessage(message)
        }

        // Reopen shop GUI
        plugin.guiManager.openNpcShopGUI(player, npcId)
    }

    private fun calculateMaxTrades(player: Player, trade: TradeData): Int {
        var maxTrades = Int.MAX_VALUE

        // Check money limit
        if (trade.requiredMoney > 0) {
            val balance = plugin.economyManager.economy?.getBalance(player) ?: 0.0
            if (balance < trade.requiredMoney) return 0
            maxTrades = minOf(maxTrades, (balance / trade.requiredMoney).toInt())
        }

        // Check points limit
        if (trade.requiredPoints > 0) {
            val points = plugin.economyManager.playerPointsAPI?.look(player.uniqueId) ?: 0
            if (points < trade.requiredPoints) return 0
            maxTrades = minOf(maxTrades, points / trade.requiredPoints)
        }

        // Check item limits
        trade.requiredItems.forEach { requiredItem ->
            if (requiredItem.type != Material.AIR) {
                val playerAmount = ItemUtils.countItems(player, requiredItem)
                if (playerAmount < requiredItem.amount) return 0
                maxTrades = minOf(maxTrades, playerAmount / requiredItem.amount)
            }
        }

        return if (maxTrades == Int.MAX_VALUE) 1 else maxTrades
    }

    private fun deleteTrade(player: Player, npcId: String, tradeId: String) {
        plugin.tradeManager.removeTrade(npcId, tradeId)
        plugin.tradeManager.saveTrades()
        player.sendMessage(plugin.configManager.getMessage("trade-deleted"))
        plugin.guiManager.openNpcTradeManagementGUI(player, npcId)
    }

    private fun saveTrade(player: Player, gui: org.bukkit.inventory.Inventory, npcId: String, tradeId: String) {
        val trade = plugin.tradeManager.getTrade(npcId, tradeId) ?: TradeData()

        // Clear existing items
        trade.requiredItems.clear()
        trade.rewardItems.clear()

        // Get items from GUI
        val requiredSlots = plugin.configManager.guiConfig.getIntegerList("trade-edit.required-item-slots")
        val rewardSlots = plugin.configManager.guiConfig.getIntegerList("trade-edit.reward-item-slots")

        requiredSlots.forEach { slot ->
            val item = gui.getItem(slot)
            if (item != null && item.type != Material.AIR) {
                trade.requiredItems.add(item.clone())
            }
        }

        rewardSlots.forEach { slot ->
            val item = gui.getItem(slot)
            if (item != null && item.type != Material.AIR) {
                trade.rewardItems.add(item.clone())
            }
        }

        plugin.tradeManager.addTrade(npcId, tradeId, trade)
        plugin.tradeManager.saveTrades()
        player.sendMessage(plugin.configManager.getMessage("trade-saved"))
        player.closeInventory()
    }

    private fun getInventoryTitle(event: InventoryClickEvent): String {
        return try {
            val view = event.view
            val getTitleMethod = view.javaClass.getMethod("getTitle")
            getTitleMethod.isAccessible = true
            getTitleMethod.invoke(view) as String
        } catch (e: Exception) {
            ""
        }
    }

    private fun getPlayerTradeSlots(player: Player): Map<Int, String> {
        // This should be implemented in GUIManager to track trade slots per player
        return emptyMap() // Placeholder
    }
}