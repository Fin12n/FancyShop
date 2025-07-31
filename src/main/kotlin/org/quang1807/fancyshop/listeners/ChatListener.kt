package org.quang1807.fancyshop.listeners

import org.quang1807.fancyshop.FancyShop
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(private val plugin: FancyShop) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val input = plugin.guiManager.getChatInput(player) ?: return

        event.isCancelled = true
        val message = event.message.trim()
        val npcId = plugin.guiManager.getEditingNpc(player)
        val tradeId = plugin.guiManager.getEditingTradeId(player)

        if (message.equals("cancel", ignoreCase = true)) {
            plugin.guiManager.removeChatInput(player)
            player.sendMessage(plugin.configManager.getMessage("currency-cancelled"))
            if (npcId != null && tradeId != null) {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    plugin.guiManager.openTradeEditGUI(player, npcId, tradeId)
                })
            }
            return
        }

        if (npcId == null || tradeId == null) {
            plugin.guiManager.removeChatInput(player)
            return
        }

        val trade = plugin.tradeManager.getTrade(npcId, tradeId)
        if (trade == null) {
            plugin.guiManager.removeChatInput(player)
            return
        }

        try {
            when {
                input.endsWith("money") -> {
                    val amount = message.toDouble()
                    if (amount < 0) {
                        player.sendMessage(plugin.configManager.getMessage("currency-negative-amount"))
                        return
                    }
                    when (input) {
                        "required_money" -> trade.requiredMoney = amount
                        "reward_money" -> trade.rewardMoney = amount
                    }
                }
                input.endsWith("points") -> {
                    val amount = message.toInt()
                    if (amount < 0) {
                        player.sendMessage(plugin.configManager.getMessage("currency-negative-amount"))
                        return
                    }
                    when (input) {
                        "required_points" -> trade.requiredPoints = amount
                        "reward_points" -> trade.rewardPoints = amount
                    }
                }
            }

            plugin.guiManager.removeChatInput(player)
            player.sendMessage(plugin.configManager.getMessage("currency-set-${input.replace("_", "-")}")
                .replace("{amount}", message))

            Bukkit.getScheduler().runTask(plugin, Runnable {
                plugin.guiManager.openTradeEditGUI(player, npcId, tradeId)
            })

        } catch (e: NumberFormatException) {
            player.sendMessage(plugin.configManager.getMessage("currency-invalid-number"))
        }
    }
}