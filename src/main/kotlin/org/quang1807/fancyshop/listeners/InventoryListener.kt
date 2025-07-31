package org.quang1807.fancyshop.listeners

import org.quang1807.fancyshop.FancyShop
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.scheduler.BukkitRunnable

class InventoryListener(private val plugin: FancyShop) : Listener {

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val closedTitle = getInventoryTitle(event)
        val shopTitle = plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("npc-shop.title", "&1&lTrao Đổi") ?: "&1&lTrao Đổi"
        )

        if (closedTitle == shopTitle) {
            object : BukkitRunnable() {
                override fun run() {
                    if (!player.isOnline) {
                        return
                    }

                    val currentTitle = try {
                        player.openInventory.topInventory.viewers.firstOrNull()?.let {
                            getInventoryTitle(event)
                        } ?: ""
                    } catch (e: Exception) {
                        ""
                    }

                    if (currentTitle != shopTitle) {
                        plugin.guiManager.cleanupPlayer(player)
                    }
                }
            }.runTaskLater(plugin, 1L)
        }

        // Clean up editing states when closing edit GUIs
        val editTitle = plugin.configManager.translateColors(
            plugin.configManager.guiConfig.getString("trade-edit.title", "&2Chỉnh sửa giao dịch") ?: "&2Chỉnh sửa giao dịch"
        )

        if (closedTitle.startsWith(editTitle.split(" -")[0])) {
            // Don't cleanup immediately on trade edit close as it might reopen
        }
    }

    private fun getInventoryTitle(event: InventoryCloseEvent): String {
        return try {
            val view = event.view
            val getTitleMethod = view.javaClass.getMethod("getTitle")
            getTitleMethod.isAccessible = true
            getTitleMethod.invoke(view) as String
        } catch (e: Exception) {
            ""
        }
    }
}