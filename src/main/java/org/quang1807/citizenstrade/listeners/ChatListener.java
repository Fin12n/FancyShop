package org.quang1807.citizenstrade.listeners;

import org.quang1807.citizenstrade.CitizenShop;
import org.quang1807.citizenstrade.models.TradeData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final CitizenShop plugin;

    public ChatListener(CitizenShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String input = plugin.getChatInput().get(player.getUniqueId());
        if (input == null) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();
        String npcId = plugin.getEditingNpc().get(player.getUniqueId());
        String tradeId = plugin.getEditingTradeId().get(player.getUniqueId());

        // Handle cancel command
        if (message.equalsIgnoreCase("cancel")) {
            plugin.getChatInput().remove(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().getMessage("currency-cancelled"));
            if (npcId != null && tradeId != null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getGuiManager().openTradeEditGUI(player, npcId, tradeId));
            }
            return;
        }

        if (npcId == null || tradeId == null) {
            plugin.getChatInput().remove(player.getUniqueId());
            return;
        }

        TradeData trade = plugin.getNpcTrades().get(npcId).get(tradeId);
        if (trade == null) {
            plugin.getChatInput().remove(player.getUniqueId());
            return;
        }

        try {
            // Handle money input
            if (input.endsWith("money")) {
                double amount = Double.parseDouble(message);
                if (amount < 0) {
                    player.sendMessage(plugin.getConfigManager().getMessage("currency-negative-amount"));
                    return;
                }

                switch (input) {
                    case "required_money":
                        trade.setRequiredMoney(amount);
                        break;
                    case "reward_money":
                        trade.setRewardMoney(amount);
                        break;
                }
            }
            // Handle points input
            else if (input.endsWith("points")) {
                int amount = Integer.parseInt(message);
                if (amount < 0) {
                    player.sendMessage(plugin.getConfigManager().getMessage("currency-negative-amount"));
                    return;
                }

                switch (input) {
                    case "required_points":
                        trade.setRequiredPoints(amount);
                        break;
                    case "reward_points":
                        trade.setRewardPoints(amount);
                        break;
                }
            }

            plugin.getChatInput().remove(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().getMessage("currency-set-" + input.replace("_", "-"))
                    .replace("{amount}", message));

            // Reopen GUI on main thread
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getGuiManager().openTradeEditGUI(player, npcId, tradeId));

        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("currency-invalid-number"));
        }
    }
}