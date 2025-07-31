package org.quang1807.fancyshop.managers

import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.Npc
import net.citizensnpcs.Citizens
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import org.quang1807.fancyshop.FancyShop
import org.bukkit.Location
import java.util.*

class NPCManager(private val plugin: FancyShop) {

    private val npcLocations = mutableMapOf<String, Location>()
    private var hasFancyNPC = false
    private var hasCitizens = false

    fun setupNPCPlugins() {
        hasFancyNPC = plugin.server.pluginManager.getPlugin("FancyNpcs") != null
        hasCitizens = plugin.server.pluginManager.getPlugin("Citizens") != null

        if (!hasFancyNPC && !hasCitizens) {
            plugin.logger.warning("Neither FancyNPCs nor Citizens found! NPC interactions disabled.")
        } else {
            plugin.logger.info("NPC Support: FancyNPCs=${hasFancyNPC}, Citizens=${hasCitizens}")
        }
    }

    fun cacheNpcLocations() {
        npcLocations.clear()

        // Cache FancyNPC locations
        if (hasFancyNPC) {
            try {
                FancyNpcsPlugin.get()?.npcManager?.allNpcs?.forEach { npc ->
                    val data = npc.data
                    if (data?.location != null) {
                        npcLocations[data.id] = data.location
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("Could not cache FancyNPC locations: ${e.message}")
            }
        }

        // Cache Citizens NPC locations
        if (hasCitizens) {
            try {
                CitizensAPI.getNPCRegistry().forEach { npc ->
                    if (npc.isSpawned) {
                        npcLocations["citizens_${npc.id}"] = npc.storedLocation
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("Could not cache Citizens NPC locations: ${e.message}")
            }
        }

        plugin.logger.info("Cached ${npcLocations.size} NPC locations")
    }

    fun getAllNPCs(): List<NPCInfo> {
        val npcs = mutableListOf<NPCInfo>()

        // Get FancyNPCs
        if (hasFancyNPC) {
            try {
                FancyNpcsPlugin.get()?.npcManager?.allNpcs?.forEach { npc ->
                    val data = npc.data
                    if (data != null) {
                        npcs.add(NPCInfo(data.id, data.name, NPCType.FANCY_NPC, data.location))
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("Error getting FancyNPCs: ${e.message}")
            }
        }

        // Get Citizens NPCs
        if (hasCitizens) {
            try {
                CitizensAPI.getNPCRegistry().forEach { npc ->
                    npcs.add(NPCInfo(
                        "citizens_${npc.id}",
                        npc.name,
                        NPCType.CITIZENS,
                        if (npc.isSpawned) npc.storedLocation else null
                    ))
                }
            } catch (e: Exception) {
                plugin.logger.warning("Error getting Citizens NPCs: ${e.message}")
            }
        }

        return npcs
    }

    fun getNPCLocation(npcId: String): Location? {
        return npcLocations[npcId]
    }

    fun isNearNPC(location: Location, npcId: String): Boolean {
        val npcLocation = npcLocations[npcId] ?: return false
        return npcLocation.world == location.world && npcLocation.distance(location) < 2.0
    }

    data class NPCInfo(
        val id: String,
        val name: String,
        val type: NPCType,
        val location: Location?
    )

    enum class NPCType {
        FANCY_NPC, CITIZENS
    }
}
