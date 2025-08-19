package org.quang1807.citizenstrade.commands;

import org.quang1807.citizenstrade.CitizenShop;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandManager implements CommandExecutor {
    private final CitizenShop plugin;

    public CommandManager(CitizenShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("citizentrade.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "edit":
                plugin.getPlayerCurrentPage().put(player.getUniqueId(), 0);
                plugin.getGuiManager().openNpcSelectionGUI(player);
                break;

            case "reload":
                plugin.reload();
                player.sendMessage(plugin.getConfigManager().getMessage("config-reloaded"));
                break;

            case "debug":
                Player target = player;
                if (args.length > 1) {
                    Player foundPlayer = Bukkit.getPlayer(args[1]);
                    if (foundPlayer != null) {
                        target = foundPlayer;
                    }
                }
                debugPlayer(player, target);
                break;

            default:
                player.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                break;
        }
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(plugin.getConfigManager().getMessage("help-header"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-edit"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-reload"));
        player.sendMessage(plugin.getConfigManager().getMessage("help-debug"));
    }

    private void debugPlayer(Player sender, Player target) {
        sender.sendMessage(plugin.getConfigManager().getMessage("debug-header").replace("{player}", target.getName()));
        sender.sendMessage(plugin.getConfigManager().getMessage("debug-chat-input")
                .replace("{value}", String.valueOf(plugin.getChatInput().get(target.getUniqueId()))));
        sender.sendMessage(plugin.getConfigManager().getMessage("debug-editing-npc")
                .replace("{value}", String.valueOf(plugin.getEditingNpc().get(target.getUniqueId()))));
        sender.sendMessage(plugin.getConfigManager().getMessage("debug-editing-trade")
                .replace("{value}", String.valueOf(plugin.getEditingTradeId().get(target.getUniqueId()))));
        sender.sendMessage(plugin.getConfigManager().getMessage("debug-current-trade-npc")
                .replace("{value}", String.valueOf(plugin.getCurrentTradeNpc().get(target.getUniqueId()))));
        sender.sendMessage(plugin.getConfigManager().getMessage("debug-current-trade-id")
                .replace("{value}", String.valueOf(plugin.getCurrentTradeId().get(target.getUniqueId()))));
        sender.sendMessage(plugin.getConfigManager().getMessage("debug-current-page")
                .replace("{value}", String.valueOf(plugin.getPlayerCurrentPage().getOrDefault(target.getUniqueId(), 0))));
        sender.sendMessage(plugin.getConfigManager().getMessage("debug-total-npcs")
                .replace("{value}", String.valueOf(plugin.getNpcTrades().size())));
    }
}