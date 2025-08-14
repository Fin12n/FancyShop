package org.quang1807.citizenstrade.listeners;

import org.quang1807.citizenstrade.CitizenShop;
import org.quang1807.citizenstrade.models.TradeData;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;

public class NPCClickListener implements Listener {
    private final CitizenShop plugin;

    public NPCClickListener(CitizenShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onNPCRightClick(NPCRightClickEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getClicker();
        String npcId = String.valueOf(event.getNPC().getId());

        // Check if this NPC has any trades configured
        Map<String, TradeData> npcTradeMap = plugin.getNpcTrades().get(npcId);

        if (npcTradeMap != null && !npcTradeMap.isEmpty()) {
            // Cancel the event to prevent other plugins from handling it
            event.setCancelled(true);

            // Open the shop GUI
            plugin.getGuiManager().openNpcShopGUI(player, npcId);
        }
    }
}