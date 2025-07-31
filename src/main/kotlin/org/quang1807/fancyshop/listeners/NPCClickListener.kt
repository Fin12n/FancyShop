package org.quang1807.fancyshop.listeners

import de.oliver.fancynpcs.api.events.NpcInteractEvent
import net.citizensnpcs.api.event.NPCRightClickEvent
import org.quang1807.fancyshop.FancyShop
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent

class NPCClickListener(private val plugin: FancyShop) : Listener {

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

    @EventHandler(priority = EventPriority.HIGH)
    fun onCitizensNpcClick(event: NPCRightClickEvent) {
        try {
            val player = event.clicker
            val npc = event.npc

            val npcId = "citizens_${npc.id}"
            val npcTrades = plugin.tradeManager.getNPCTrades(npcId)

            if (!npcTrades.isNullOrEmpty()) {
                event.isCancelled = true
                plugin.guiManager.openNpcShopGUI(player, npcId)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error handling Citizens NPC interaction: ${e.message}")
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val clickedEntity = event.rightClicked
        val clickedLocation = clickedEntity.location

        // Check if clicked entity is near any cached NPC location
        plugin.npcManager.getAllNPCs().forEach { npcInfo ->
            if (npcInfo.location != null &&
                npcInfo.location.world == clickedLocation.world &&
                npcInfo.location.distance(clickedLocation) < 2.0) {

                val npcTrades = plugin.tradeManager.getNPCTrades(npcInfo.id)
                if (!npcTrades.isNullOrEmpty()) {
                    event.isCancelled = true
                    plugin.guiManager.openNpcShopGUI(player, npcInfo.id)
                    return
                }
            }
        }
    }
}