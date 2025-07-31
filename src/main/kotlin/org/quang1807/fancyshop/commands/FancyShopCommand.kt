package org.quang1807.fancyshop.commands

import org.quang1807.fancyshop.FancyShop
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class FancyShopCommand(private val plugin: FancyShop) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.configManager.getMessage("only-players"))
            return true
        }

        if (!sender.hasPermission("fancyshop.admin")) {
            sender.sendMessage(plugin.configManager.getMessage("no-permission"))
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {
            "edit" -> {
                plugin.guiManager.openNpcSelectionGUI(sender)
            }
            "reload" -> {
                plugin.configManager.loadConfigs()
                plugin.tradeManager.loadTrades()
                plugin.npcManager.cacheNpcLocations()
                sender.sendMessage(plugin.configManager.getMessage("config-reloaded"))
            }
            "debug" -> {
                val target = if (args.size > 1) Bukkit.getPlayer(args[1]) ?: sender else sender
                debugPlayer(sender, target)
            }
            else -> {
                sendHelpMessage(sender)
            }
        }

        return true
    }

    private fun sendHelpMessage(player: Player) {
        player.sendMessage(plugin.configManager.getMessage("help-header"))
        player.sendMessage(plugin.configManager.getMessage("help-edit"))
        player.sendMessage(plugin.configManager.getMessage("help-reload"))
        player.sendMessage(plugin.configManager.getMessage("help-debug"))
    }

    private fun debugPlayer(sender: Player, target: Player) {
        val guiManager = plugin.guiManager
        sender.sendMessage(plugin.configManager.getMessage("debug-header").replace("{player}", target.name))
        sender.sendMessage(plugin.configManager.getMessage("debug-editing-npc").replace("{value}", guiManager.getEditingNpc(target) ?: "null"))
        sender.sendMessage(plugin.configManager.getMessage("debug-editing-trade").replace("{value}", guiManager.getEditingTradeId(target) ?: "null"))
        sender.sendMessage(plugin.configManager.getMessage("debug-current-trade-npc").replace("{value}", guiManager.getCurrentTradeNpc(target) ?: "null"))
        sender.sendMessage(plugin.configManager.getMessage("debug-current-trade-id").replace("{value}", guiManager.getCurrentTradeId(target) ?: "null"))
        sender.sendMessage(plugin.configManager.getMessage("debug-total-npcs").replace("{value}", plugin.tradeManager.getAllNPCTrades().size.toString()))
    }
}