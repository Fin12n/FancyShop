package org.quang1807.fancyshop.listeners

import de.oliver.fancynpcs.api.events.NpcInteractEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.quang1807.fancyshop.FancyShop

class FancyNpcClickListener(private val plugin: FancyShop) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onFancyNpcInteract(event: NpcInteractEvent) {
        try {
            val player = event.player
            val npc = event.npc
            if (npc?.data == null) return

            val npcId = npc.data.id
            val npcTrades = plugin.tradeManager.getNPCTrades(npcId)

            if (!npcTrades.isNullOrEmpty()) {
                event.isCancelled = true
                plugin.guiManager.openNpcShopGUI(player, npcId)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error handling FancyNPC interaction: ${e.message}")
        }
    }
}
