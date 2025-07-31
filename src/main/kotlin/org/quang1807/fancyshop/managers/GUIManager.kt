package org.quang1807.fancyshop.managers

import org.quang1807.fancyshop.FancyShop
import org.quang1807.fancyshop.data.TradeData
import org.quang1807.fancyshop.utils.ItemUtils
import org.quang1807.fancyshop.utils.SkullUtils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

class GUIManager(private val plugin: FancyShop) {

    // Player states
    private val editingNpc = mutableMapOf<UUID, String>()
    private val editingTradeId = mutableMapOf<UUID, String>()
    private val chatInput = mutableMapOf<UUID, String>()
    private val currentTradeNpc = mutableMapOf<UUID, String>()
    private val currentTradeId = mutableMapOf<UUID, String>()
    private val playerCurrentPage = mutableMapOf<UUID, Int>()
    private val playerShopPage = mutableMapOf<UUID, Int>()
    private val playerTradeSlots = mutableMapOf<UUID, MutableMap<Int, String>>()

    companion object {
        private const val TRADES_PER_PAGE = 5
    }

    // Getter methods for debugging
    fun getEditingNpc(player: Player): String? = editingNpc[player.uniqueId]
    fun getEditingTradeId(player: Player): String? = editingTradeId[player.uniqueId]
    fun getCurrentTradeNpc(player: Player): String? = currentTradeNpc[player.uniqueId]
    fun getCurrentTradeId(player: Player): String? = currentTradeId[player.uniqueId]
    fun getChatInput(player: Player): String? = chatInput[player.uniqueId]

    // Chat input management
    fun setChatInput(player: Player, input: String) {
        chatInput[player.uniqueId] = input
    }

    fun removeChatInput(player: Player) {
        chatInput.remove(player.uniqueId)
    }

    fun openNpcSelectionGUI(player: Player) {
        val allNpcs = plugin.npcManager.getAllNPCs()

        if (allNpcs.isEmpty()) {
            player.sendMessage(plugin.configManager.getMessage("no-npcs-found"))
            return
        }

        val title = plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("npc-selection.title", "&9Chọn NPC để chỉnh sửa") ?: "&9Chọn NPC để chỉnh sửa"
        )
        val size = plugin.configManager.guiConfig.getInt("npc-selection.size", 54)
        val gui = Bukkit.createInventory(null, size, title)

        fillDecorative(gui, "npc-selection")

        val npcSlots = plugin.configManager.guiConfig.getIntegerList("npc-selection.npc-slots").takeIf { it.isNotEmpty() }
            ?: listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34)

        val currentPage = playerCurrentPage.getOrDefault(player.uniqueId, 0)
        val itemsPerPage = npcSlots.size
        val totalPages = (allNpcs.size + itemsPerPage - 1) / itemsPerPage
        val startIndex = currentPage * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, allNpcs.size)

        val npcsOnPage = allNpcs.subList(startIndex, endIndex)

        npcsOnPage.forEachIndexed { index, npcInfo ->
            if (index >= npcSlots.size) return@forEachIndexed

            val item = createConfiguredItem("npc-selection.npc-item", npcInfo.name, npcInfo.id)
            val meta = item.itemMeta
            if (meta != null) {
                val lore = mutableListOf<String>()
                plugin.configManager.guiConfig.getStringList("npc-selection.npc-item.lore").forEach { line ->
                    var processedLine = line.replace("{npc_id}", npcInfo.id)
                        .replace("{npc_name}", npcInfo.name)

                    val npcTrades = plugin.tradeManager.getNPCTrades(npcInfo.id)
                    if (npcTrades?.isNotEmpty() == true) {
                        processedLine = processedLine.replace("{status}",
                            plugin.configManager.guiConfig.getString("npc-selection.status.configured", "&a✓ Đã cấu hình giao dịch") ?: "&a✓ Đã cấu hình")
                        processedLine = processedLine.replace("{trade_count}", npcTrades.size.toString())
                    } else {
                        processedLine = processedLine.replace("{status}",
                            plugin.configManager.guiConfig.getString("npc-selection.status.not-configured", "&c✗ Chưa cấu hình giao dịch") ?: "&c✗ Chưa cấu hình")
                        processedLine = processedLine.replace("{trade_count}", "0")
                    }
                    lore.add(plugin.configManager.translateColors(processedLine))
                }
                meta.lore = lore
                item.itemMeta = meta
            }
            gui.setItem(npcSlots[index], item)
        }

        // Pagination buttons
        if (currentPage > 0) {
            val prevButtonSlot = plugin.configManager.guiConfig.getInt("npc-selection.pagination.prev-button.slot", 48)
            val prevButton = createConfiguredItem("npc-selection.pagination.prev-button", "", "")
            gui.setItem(prevButtonSlot, prevButton)
        }

        if (currentPage < totalPages - 1) {
            val nextButtonSlot = plugin.configManager.guiConfig.getInt("npc-selection.pagination.next-button.slot", 50)
            val nextButton = createConfiguredItem("npc-selection.pagination.next-button", "", "")
            gui.setItem(nextButtonSlot, nextButton)
        }

        // Page info
        val pageInfoSlot = plugin.configManager.guiConfig.getInt("npc-selection.pagination.page-info-item.slot", 49)
        val pageInfoItem = createConfiguredItem("npc-selection.pagination.page-info-item", "", "")
        val pageInfoMeta = pageInfoItem.itemMeta
        if (pageInfoMeta != null) {
            var displayName = plugin.configManager.guiConfig.getString("npc-selection.pagination.page-info-item.name", "&e&lTrang {current_page}/{total_pages}") ?: "&e&lTrang {current_page}/{total_pages}"
            displayName = displayName.replace("{current_page}", (currentPage + 1).toString())
                .replace("{total_pages}", totalPages.toString())
            pageInfoMeta.setDisplayName(plugin.configManager.translateColors(displayName))

            val lore = mutableListOf<String>()
            plugin.configManager.guiConfig.getStringList("npc-selection.pagination.page-info-item.lore").forEach { line ->
                val processedLine = line.replace("{total_npcs}", allNpcs.size.toString())
                lore.add(plugin.configManager.translateColors(processedLine))
            }
            pageInfoMeta.lore = lore
            pageInfoItem.itemMeta = pageInfoMeta
        }
        gui.setItem(pageInfoSlot, pageInfoItem)

        player.openInventory(gui)
    }

    fun openNpcTradeManagementGUI(player: Player, npcId: String) {
        val title = plugin.configManager.translateColors(
            (plugin.configManager.guiConfig.getString("npc-trade-management.title", "&6Quản lý giao dịch NPC") ?: "&6Quản lý giao dịch NPC")
                .replace("{npc_id}", npcId)
        )
        val size = plugin.configManager.guiConfig.getInt("npc-trade-management.size", 54)
        val gui = Bukkit.createInventory(null, size, title)

        fillDecorative(gui, "npc-trade-management")

        val npcTrades = plugin.tradeManager.getNPCTrades(npcId) ?: emptyMap()
        val tradeSlots = listOf(2, 5, 11, 14, 20, 23, 29, 32, 38, 41, 47, 50)

        npcTrades.entries.take(tradeSlots.size).forEachIndexed { index, (tradeId, trade) ->
            val displayItem = createTradeDisplayItem(trade)
            val meta = displayItem.itemMeta
            if (meta != null) {
                val lore = meta.lore?.toMutableList() ?: mutableListOf()
                lore.add("")
                lore.add(plugin.configManager.translateColors("&7Trade ID: &f$tradeId"))
                lore.add(plugin.configManager.translateColors("&e&lClick trái: &7Chỉnh sửa giao dịch"))
                lore.add(plugin.configManager.translateColors("&c&lClick phải: &7Xóa giao dịch"))
                meta.lore = lore
                displayItem.itemMeta = meta
            }
            gui.setItem(tradeSlots[index], displayItem)
        }

        // Add button
        val addButtonSlot = plugin.configManager.guiConfig.getInt("npc-trade-management.add-button.slot", 40)
        val addButton = createConfiguredItem("npc-trade-management.add-button", "", "")
        gui.setItem(addButtonSlot, addButton)

        // Back button
        val backSlot = plugin.configManager.guiConfig.getInt("npc-trade-management.back-button.slot", 4)
        val backButton = createConfiguredItem("npc-trade-management.back-button", "", "")
        gui.setItem(backSlot, backButton)

        editingNpc[player.uniqueId] = npcId
        player.openInventory(gui)
    }

    fun openTradeEditGUI(player: Player, npcId: String, tradeId: String) {
        val title = plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("trade-edit.title", "&2Chỉnh sửa giao dịch") ?: "&2Chỉnh sửa giao dịch"
        )
        val gui = Bukkit.createInventory(null, 54, title)
        fillDecorative(gui, "trade-edit")

        val trade = plugin.tradeManager.getTrade(npcId, tradeId) ?: TradeData().also {
            plugin.tradeManager.addTrade(npcId, tradeId, it)
        }

        // Required item slots
        val requiredItemSlots = plugin.configManager.guiConfig.getIntegerList("trade-edit.required-item-slots")
        trade.requiredItems.take(requiredItemSlots.size).forEachIndexed { index, item ->
            gui.setItem(requiredItemSlots[index], item)
        }

        // Reward item slots
        val rewardSlots = plugin.configManager.guiConfig.getIntegerList("trade-edit.reward-item-slots")
        trade.rewardItems.take(rewardSlots.size).forEachIndexed { index, item ->
            gui.setItem(rewardSlots[index], item)
        }

        // Currency slots
        val reqMoneyHead = if (trade.requiredMoney > 0) SkullUtils.createGoldHead(trade.requiredMoney, 0, plugin.configManager) else null
        gui.setItem(plugin.configManager.guiConfig.getInt("trade-edit.required-money-slot"),
            reqMoneyHead ?: createConfiguredItem("trade-edit.add-money-item", "", ""))

        val reqPointHead = if (trade.requiredPoints > 0) SkullUtils.createPointHead(trade.requiredPoints, plugin.configManager) else null
        gui.setItem(plugin.configManager.guiConfig.getInt("trade-edit.required-points-slot"),
            reqPointHead ?: createConfiguredItem("trade-edit.add-points-item", "", ""))

        val rewardMoneyHead = if (trade.rewardMoney > 0) SkullUtils.createGoldHead(trade.rewardMoney, 0, plugin.configManager) else null
        gui.setItem(plugin.configManager.guiConfig.getInt("trade-edit.reward-money-slot"),
            rewardMoneyHead ?: createConfiguredItem("trade-edit.add-money-item", "", ""))

        val rewardPointHead = if (trade.rewardPoints > 0) SkullUtils.createPointHead(trade.rewardPoints, plugin.configManager) else null
        gui.setItem(plugin.configManager.guiConfig.getInt("trade-edit.reward-points-slot"),
            rewardPointHead ?: createConfiguredItem("trade-edit.add-points-item", "", ""))

        // Control buttons
        gui.setItem(plugin.configManager.guiConfig.getInt("trade-edit.save-button.slot"),
            createConfiguredItem("trade-edit.save-button", "", ""))
        gui.setItem(plugin.configManager.guiConfig.getInt("trade-edit.back-button.slot"),
            createConfiguredItem("trade-edit.back-button", "", ""))

        editingNpc[player.uniqueId] = npcId
        editingTradeId[player.uniqueId] = tradeId
        player.openInventory(gui)
    }

    fun openNpcShopGUI(player: Player, npcId: String) {
        val npcTrades = plugin.tradeManager.getNPCTrades(npcId)
        if (npcTrades.isNullOrEmpty()) {
            player.sendMessage(plugin.configManager.getMessage("shop-no-trades"))
            return
        }

        val title = plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("npc-shop.title", "&1&lTrao Đổi") ?: "&1&lTrao Đổi"
        )
        val gui = Bukkit.createInventory(player, 54, title)
        fillDecorative(gui, "npc-shop")

        val sortedTradeIds = npcTrades.keys.sorted()
        val currentPage = playerShopPage.getOrDefault(player.uniqueId, 0)
        val totalTrades = sortedTradeIds.size
        val totalPages = (totalTrades + TRADES_PER_PAGE - 1) / TRADES_PER_PAGE

        if (currentPage >= totalPages && totalPages > 0) {
            playerShopPage[player.uniqueId] = totalPages - 1
        }

        val startIndex = currentPage * TRADES_PER_PAGE
        val endIndex = minOf(startIndex + TRADES_PER_PAGE, totalTrades)

        val tradeSlotsForPlayer = mutableMapOf<Int, String>()

        for (i in startIndex until endIndex) {
            val tradeId = sortedTradeIds[i]
            val row = i % TRADES_PER_PAGE
            drawTradeRow(gui, player, npcTrades[tradeId]!!, row)
            tradeSlotsForPlayer[5 + (row * 9)] = tradeId
        }

        playerTradeSlots[player.uniqueId] = tradeSlotsForPlayer
        drawControlPanel(gui, player, currentPage, totalPages)

        currentTradeNpc[player.uniqueId] = npcId
        player.openInventory(gui)
    }

    private fun drawTradeRow(gui: Inventory, player: Player, trade: TradeData, row: Int) {
        val startSlot = row * 9
        val required = mutableListOf<ItemStack>()

        // Add required items
        trade.requiredItems.filter { it.type != Material.AIR }.forEach { required.add(it) }

        // Add currency requirements
        if (trade.requiredMoney > 0) {
            SkullUtils.createGoldHead(trade.requiredMoney, 0, plugin.configManager)?.let { required.add(it) }
        }
        if (trade.requiredPoints > 0) {
            SkullUtils.createPointHead(trade.requiredPoints, plugin.configManager)?.let { required.add(it) }
        }

        // Place required items
        required.take(2).forEachIndexed { index, item ->
            gui.setItem(startSlot + 2 + index, item)
        }

        // Arrow
        val arrow = createConfiguredItem("npc-shop.arrow-item", "", "")
        gui.setItem(startSlot + 4, arrow)

        // Reward item
        val rewardItem = createTradeDisplayItem(trade)
        gui.setItem(startSlot + 5, rewardItem)
    }

    private fun drawControlPanel(gui: Inventory, player: Player, currentPage: Int, totalPages: Int) {
        val lastColumn = 7

        // Previous page button
        if (currentPage > 0) {
            gui.setItem(lastColumn, createConfiguredItem("npc-shop.control-panel.prev-page", "", ""))
        } else {
            gui.setItem(lastColumn, createConfiguredItem("npc-shop.control-panel.placeholder", "", ""))
        }

        // Page info
        val pageInfo = createConfiguredItem("npc-shop.control-panel.page-info", "", "")
        val pageInfoMeta = pageInfo.itemMeta
        if (pageInfoMeta != null) {
            val name = pageInfoMeta.displayName
                .replace("{current_page}", (currentPage + 1).toString())
                .replace("{total_pages}", if (totalPages == 0) "1" else totalPages.toString())
            pageInfoMeta.setDisplayName(plugin.configManager.translateColors(name))
            pageInfo.itemMeta = pageInfoMeta
        }
        gui.setItem(lastColumn + 9, pageInfo)

        // Next page button
        if (currentPage < totalPages - 1) {
            gui.setItem(lastColumn + 18, createConfiguredItem("npc-shop.control-panel.next-page", "", ""))
        } else {
            gui.setItem(lastColumn + 18, createConfiguredItem("npc-shop.control-panel.placeholder", "", ""))
        }

        // Player info with skull
        val playerInfo = createConfiguredItem("npc-shop.control-panel.player-info", player.name, "")
        ItemUtils.setPlayerSkull(playerInfo, player)
        val playerInfoMeta = playerInfo.itemMeta
        if (playerInfoMeta != null) {
            val lore = mutableListOf<String>()
            lore.add(" ")
            lore.add(plugin.configManager.translateColors(
                plugin.configManager.getMessage("shop-player-money")
                    .replace("{amount}", String.format("%,.0f", plugin.economyManager.economy?.getBalance(player) ?: 0.0))
            ))
            plugin.economyManager.playerPointsAPI?.let { api ->
                lore.add(plugin.configManager.translateColors(
                    plugin.configManager.getMessage("shop-player-points")
                        .replace("{amount}", String.format("%,d", api.look(player.uniqueId)))
                ))
            }
            playerInfoMeta.lore = lore
            playerInfo.itemMeta = playerInfoMeta
        }
        gui.setItem(lastColumn + 27, playerInfo)

        // Help and close buttons
        gui.setItem(lastColumn + 36, createConfiguredItem("npc-shop.control-panel.help", "", ""))
        gui.setItem(lastColumn + 45, createConfiguredItem("npc-shop.control-panel.close", "", ""))
    }

    private fun createTradeDisplayItem(trade: TradeData): ItemStack {
        // Try to get first reward item
        val firstRewardItem = trade.rewardItems.firstOrNull { it.type != Material.AIR }
        if (firstRewardItem != null) {
            val displayItem = firstRewardItem.clone()
            val meta = displayItem.itemMeta
            if (meta != null) {
                val lore = meta.lore?.toMutableList() ?: mutableListOf()
                lore.add(" ")

                // Count other items
                val otherItemCount = trade.rewardItems.count { it.type != Material.AIR } - 1

                // Add extra rewards info
                if (trade.rewardMoney > 0) {
                    lore.add(plugin.configManager.getMessage("shop-extra-reward-money")
                        .replace("{amount}", String.format("%,.0f", trade.rewardMoney)))
                }
                if (trade.rewardPoints > 0) {
                    lore.add(plugin.configManager.getMessage("shop-extra-reward-points")
                        .replace("{amount}", String.format("%,d", trade.rewardPoints)))
                }
                if (otherItemCount > 0) {
                    lore.add(plugin.configManager.getMessage("shop-extra-reward-items")
                        .replace("{count}", otherItemCount.toString()))
                }

                meta.lore = lore
                displayItem.itemMeta = meta
            }
            return displayItem
        }

        // If no items, show money or points
        if (trade.rewardMoney > 0) {
            val title = plugin.configManager.getMessage("shop-reward-money-title")
                .replace("{amount}", String.format("%,.0f", trade.rewardMoney))
            val lore = mutableListOf(
                plugin.configManager.getMessage("shop-reward-money-lore")
                    .replace("{amount}", String.format("%,.0f", trade.rewardMoney))
            )
            if (trade.rewardPoints > 0) {
                lore.add(plugin.configManager.getMessage("shop-extra-reward-points")
                    .replace("{amount}", String.format("%,d", trade.rewardPoints)))
            }
            return SkullUtils.createCustomHead(SkullUtils.GOLD_HEAD_TEXTURE, title, lore, plugin.configManager)
        }

        if (trade.rewardPoints > 0) {
            val title = plugin.configManager.getMessage("shop-reward-points-title")
                .replace("{amount}", String.format("%,d", trade.rewardPoints))
            val lore = listOf(
                plugin.configManager.getMessage("shop-reward-points-lore")
                    .replace("{amount}", String.format("%,d", trade.rewardPoints))
            )
            return SkullUtils.createCustomHead(SkullUtils.POINT_HEAD_TEXTURE, title, lore, plugin.configManager)
        }

        // Default empty trade item
        val barrier = ItemStack(Material.BARRIER)
        val meta = barrier.itemMeta
        if (meta != null) {
            meta.setDisplayName(plugin.configManager.translateColors("&c&lGiao dịch trống"))
            meta.lore = listOf(plugin.configManager.translateColors("&7Giao dịch này chưa được cấu hình"))
            barrier.itemMeta = meta
        }
        return barrier
    }

    private fun fillDecorative(gui: Inventory, section: String) {
        val decorSection = plugin.configManager.guiConfig.getConfigurationSection("$section.decorative") ?: return

        for (key in decorSection.getKeys(false)) {
            val itemSection = decorSection.getConfigurationSection(key) ?: continue
            val slots = itemSection.getIntegerList("slots")
            val item = createConfiguredItem("$section.decorative.$key", "", "")
            slots.forEach { slot ->
                if (slot in 0 until gui.size) {
                    gui.setItem(slot, item)
                }
            }
        }
    }

    private fun createConfiguredItem(path: String, npcName: String, npcId: String): ItemStack {
        val section = plugin.configManager.guiConfig.getConfigurationSection(path)
        if (section == null) {
            return ItemStack(Material.STONE)
        }

        val materialName = section.getString("material", "STONE") ?: "STONE"
        val material = try {
            Material.valueOf(materialName.uppercase())
        } catch (e: IllegalArgumentException) {
            Material.STONE
        }

        val item = ItemStack(material)
        val meta = item.itemMeta
        if (meta != null) {
            var displayName = section.getString("name", "") ?: ""
            displayName = displayName.replace("{npc_name}", npcName).replace("{npc_id}", npcId)
            meta.setDisplayName(plugin.configManager.translateColors(displayName))

            val lore = mutableListOf<String>()
            section.getStringList("lore").forEach { line ->
                val processedLine = line.replace("{npc_name}", npcName).replace("{npc_id}", npcId)
                lore.add(plugin.configManager.translateColors(processedLine))
            }
            meta.lore = lore
            item.itemMeta = meta
        }

        return item
    }

    // Cleanup methods
    fun cleanupPlayer(player: Player) {
        val uuid = player.uniqueId
        editingNpc.remove(uuid)
        editingTradeId.remove(uuid)
        chatInput.remove(uuid)
        currentTradeNpc.remove(uuid)
        currentTradeId.remove(uuid)
        playerCurrentPage.remove(uuid)
        playerShopPage.remove(uuid)
        playerTradeSlots.remove(uuid)
    }

    // Navigation methods
    fun handleNpcSelectionClick(player: Player, slot: Int, clickedItem: ItemStack?) {
        val prevButtonSlot = plugin.configManager.guiConfig.getInt("npc-selection.pagination.prev-button.slot", 48)
        val nextButtonSlot = plugin.configManager.guiConfig.getInt("npc-selection.pagination.next-button.slot", 50)

        when (slot) {
            prevButtonSlot -> {
                val currentPage = playerCurrentPage.getOrDefault(player.uniqueId, 0)
                if (currentPage > 0) {
                    playerCurrentPage[player.uniqueId] = currentPage - 1
                    openNpcSelectionGUI(player)
                }
            }
            nextButtonSlot -> {
                val currentPage = playerCurrentPage.getOrDefault(player.uniqueId, 0)
                val allNpcs = plugin.npcManager.getAllNPCs()
                val npcSlots = plugin.configManager.guiConfig.getIntegerList("npc-selection.npc-slots").takeIf { it.isNotEmpty() }
                    ?: listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34)
                val itemsPerPage = npcSlots.size
                val totalPages = (allNpcs.size + itemsPerPage - 1) / itemsPerPage
                if (currentPage < totalPages - 1) {
                    playerCurrentPage[player.uniqueId] = currentPage + 1
                    openNpcSelectionGUI(player)
                }
            }
            else -> {
                // Handle NPC selection
                clickedItem?.itemMeta?.lore?.let { lore ->
                    for (line in lore) {
                        val cleanLine = ChatColor.stripColor(line)
                        if (cleanLine.startsWith("ID: ")) {
                            val npcId = cleanLine.substring(4)
                            openNpcTradeManagementGUI(player, npcId)
                            break
                        }
                    }
                }
            }
        }
    }
}