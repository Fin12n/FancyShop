package org.quang1807.fancyshop.listeners

import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.entity.Player
import org.bukkit.ChatColor
import org.quang1807.fancyshop.FancyShop

class InventoryClickListener(private val plugin: FancyShop) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val clickedItem = event.currentItem ?: return
        val view = event.view

        val guiTitle = ChatColor.stripColor(view.title) ?: return
        val shopTitle = ChatColor.stripColor(plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("npc-shop.title", "&1&lTrao Đổi") ?: "&1&lTrao Đổi"
        ))

        if (!guiTitle.equals(shopTitle, ignoreCase = true)) return

        event.isCancelled = true

        val npcId = plugin.guiManager.getOpenedNpcId(player) ?: return
        val trade = plugin.tradeManager.getTradeBySlot(npcId, event.rawSlot) ?: return

        if (trade.canAfford(player)) {
            trade.execute(player)
            player.sendMessage("§aBạn đã trao đổi thành công!")
        } else {
            player.sendMessage("§cBạn không đủ điều kiện để trao đổi.")
        }
    }
}
