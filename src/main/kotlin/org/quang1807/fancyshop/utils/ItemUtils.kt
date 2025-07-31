package org.quang1807.fancyshop.utils

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

object ItemUtils {

    fun countItems(player: Player, item: ItemStack): Int {
        var count = 0
        player.inventory.contents.forEach { playerItem ->
            if (playerItem != null && playerItem.isSimilar(item)) {
                count += playerItem.amount
            }
        }
        return count
    }

    fun removeItems(player: Player, items: List<ItemStack>, times: Int = 1) {
        items.forEach { item ->
            if (item.type != Material.AIR) {
                val totalToRemove = item.clone()
                totalToRemove.amount = item.amount * times
                player.inventory.removeItem(totalToRemove)
            }
        }
        player.updateInventory()
    }

    fun hasRequiredItems(player: Player, required: List<ItemStack>, times: Int = 1): Boolean {
        if (required.isEmpty() || times <= 0) return true

        required.forEach { requiredItem ->
            if (requiredItem.type != Material.AIR) {
                val totalRequired = requiredItem.amount * times
                if (countItems(player, requiredItem) < totalRequired) {
                    return false
                }
            }
        }
        return true
    }

    fun giveItems(player: Player, items: List<ItemStack>, times: Int = 1) {
        repeat(times) {
            items.forEach { item ->
                if (item.type != Material.AIR) {
                    val leftover = player.inventory.addItem(item.clone())
                    if (leftover.isNotEmpty()) {
                        leftover.values.forEach { drop ->
                            player.world.dropItemNaturally(player.location, drop)
                        }
                    }
                }
            }
        }
    }

    fun setPlayerSkull(item: ItemStack, player: Player) {
        if (item.type == Material.PLAYER_HEAD) {
            val meta = item.itemMeta as? SkullMeta
            meta?.owningPlayer = player
            item.itemMeta = meta
        }
    }
}