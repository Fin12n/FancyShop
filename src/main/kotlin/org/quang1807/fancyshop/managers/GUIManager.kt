package org.quang1807.fancyshop.managers

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.quang1807.fancyshop.FancyShop
import org.quang1807.fancyshop.objects.Trade

import java.util.*

class GUIManager(private val plugin: FancyShop) {

    private val openedNpcByPlayer = mutableMapOf<UUID, String>()

    fun openNpcShopGUI(player: Player, npcId: String) {
        val trades = plugin.tradeManager.getNPCTrades(npcId)
        if (trades.isNullOrEmpty()) {
            player.sendMessage("§cNPC này không có giao dịch nào.")
            return
        }

        val invSize = ((trades.size - 1) / 9 + 1) * 9
        val guiTitle = plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("npc-shop.title", "&1&lTrao Đổi") ?: "&1&lTrao Đổi"
        )

        val inv = Bukkit.createInventory(null, invSize, guiTitle)

        trades.forEach { trade ->
            val item = trade.displayItem.clone()
            val meta = item.itemMeta
            if (meta != null) {
                meta.lore = trade.buildLore(plugin)
                item.itemMeta = meta
            }
            inv.setItem(trade.slot, item)
        }

        // Ghi nhớ NPC đang mở
        openedNpcByPlayer[player.uniqueId] = npcId
        player.openInventory(inv)
    }

    fun getOpenedNpcId(player: Player): String? {
        return openedNpcByPlayer[player.uniqueId]
    }

    fun cleanupPlayer(player: Player) {
        openedNpcByPlayer.remove(player.uniqueId)
    }
}
