package org.quang1807.citizenstrade.listeners;

import org.quang1807.citizenstrade.CitizenShop;
import org.quang1807.citizenstrade.models.TradeData;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.*;

public class InventoryListener implements Listener {
    private final CitizenShop plugin;

    public InventoryListener(CitizenShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = getInventoryTitle(event);
        UUID playerUUID = player.getUniqueId();

        // Get GUI titles from config
        String npcSelectionTitle = plugin.getConfigManager().translateColors(
                plugin.getConfigManager().getGuiConfig().getString("npc-selection.title", "&9Chọn NPC để chỉnh sửa"));
        String npcShopTitle = plugin.getConfigManager().translateColors(
                plugin.getConfigManager().getGuiConfig().getString("npc-shop.title", "&1&lTrao Đổi"));
        String editTitleRaw = plugin.getConfigManager().getGuiConfig().getString("trade-edit.title", "&2Chỉnh sửa giao dịch");
        String tradeManagementTitleRaw = plugin.getConfigManager().getGuiConfig().getString("npc-trade-management.title", "&6&lQuản lý: {npc_id}");

        // Handle different GUI types
        if (title.equals(npcSelectionTitle)) {
            event.setCancelled(true);
            handleNpcSelection(player, event);
        } else if (title.equals(npcShopTitle)) {
            event.setCancelled(true);
            handleNpcShopClick(player, event);
        } else if (title.startsWith(plugin.getConfigManager().translateColors(tradeManagementTitleRaw.split("\\{")[0]))
                && plugin.getEditingNpc().containsKey(playerUUID)) {
            event.setCancelled(true);
            handleNpcTradeManagement(player, event);
        } else if (title.startsWith(plugin.getConfigManager().translateColors(editTitleRaw))
                && plugin.getEditingTradeId().containsKey(playerUUID)) {
            handleTradeEditClick(player, event);
        }
    }

    private void handleNpcSelection(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int clickedSlot = event.getSlot();

        // Handle pagination
        int prevButtonSlot = plugin.getConfigManager().getGuiConfig().getInt("npc-selection.pagination.prev-button.slot", 48);
        int nextButtonSlot = plugin.getConfigManager().getGuiConfig().getInt("npc-selection.pagination.next-button.slot", 50);

        if (clickedSlot == prevButtonSlot) {
            int currentPage = plugin.getPlayerCurrentPage().getOrDefault(player.getUniqueId(), 0);
            if (currentPage > 0) {
                plugin.getPlayerCurrentPage().put(player.getUniqueId(), currentPage - 1);
                plugin.getGuiManager().openNpcSelectionGUI(player);
            }
            return;
        }

        if (clickedSlot == nextButtonSlot) {
            // Calculate total pages properly
            NPCRegistry registry = CitizensAPI.getNPCRegistry();
            if (registry != null) {
                List<NPC> allNpcs = new ArrayList<>();
                for (NPC npc : registry) {
                    if (npc != null && npc.getName() != null) {
                        allNpcs.add(npc);
                    }
                }

                List<Integer> npcSlots = plugin.getConfigManager().getGuiConfig().getIntegerList("npc-selection.npc-slots");
                if (npcSlots.isEmpty()) {
                    npcSlots = Arrays.asList(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
                }

                int itemsPerPage = npcSlots.size();
                int totalPages = (int) Math.ceil((double) allNpcs.size() / itemsPerPage);
                int currentPage = plugin.getPlayerCurrentPage().getOrDefault(player.getUniqueId(), 0);

                if (currentPage < totalPages - 1) {
                    plugin.getPlayerCurrentPage().put(player.getUniqueId(), currentPage + 1);
                    plugin.getGuiManager().openNpcSelectionGUI(player);
                }
            }
            return;
        }

        // Handle NPC selection
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getLore() == null) return;

        String npcId = null;
        for (String line : meta.getLore()) {
            String cleanLine = ChatColor.stripColor(line);
            if (cleanLine.startsWith("ID: ")) {
                npcId = cleanLine.substring(4);
                break;
            }
        }

        if (npcId != null) {
            plugin.getGuiManager().openNpcTradeManagementGUI(player, npcId);
        }
    }

    private void handleNpcTradeManagement(Player player, InventoryClickEvent event) {
        int clickedSlot = event.getSlot();
        String npcId = plugin.getEditingNpc().get(player.getUniqueId());
        if (npcId == null) return;

        // Handle back button
        if (clickedSlot == plugin.getConfigManager().getGuiConfig().getInt("npc-trade-management.back-button.slot", 4)) {
            plugin.getGuiManager().openNpcSelectionGUI(player);
            return;
        }

        // Handle add button
        if (clickedSlot == plugin.getConfigManager().getGuiConfig().getInt("npc-trade-management.add-button.slot", 40)) {
            // Generate sequential trade ID
            Map<String, TradeData> npcTradeMap = plugin.getNpcTrades().getOrDefault(npcId, new HashMap<>());
            String newTradeId = generateNextTradeId(npcTradeMap.keySet());
            plugin.getGuiManager().openTradeEditGUI(player, npcId, newTradeId);
            return;
        }

        // Handle trade selection
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasLore()) return;

        String tradeId = null;
        for (String line : meta.getLore()) {
            String cleanLine = ChatColor.stripColor(line);
            if (cleanLine.startsWith("Trade ID: ")) {
                tradeId = cleanLine.substring(10);
                break;
            }
        }

        if (tradeId != null) {
            if (event.getClick() == ClickType.RIGHT) {
                deleteTrade(player, npcId, tradeId);
            } else {
                plugin.getGuiManager().openTradeEditGUI(player, npcId, tradeId);
            }
        }
    }

    private void handleNpcShopClick(Player player, InventoryClickEvent event) {
        int clickedSlot = event.getSlot();
        String npcId = plugin.getCurrentTradeNpc().get(player.getUniqueId());
        if (npcId == null) return;

        // Handle navigation with explicit slot numbers
        if (clickedSlot == 7) { // Previous page
            int currentPage = plugin.getPlayerShopPage().getOrDefault(player.getUniqueId(), 0);
            if (currentPage > 0) {
                plugin.getPlayerShopPage().put(player.getUniqueId(), currentPage - 1);
                plugin.getGuiManager().openNpcShopGUI(player, npcId);
            }
            return;
        }

        if (clickedSlot == 25) { // Next page
            Map<String, TradeData> trades = plugin.getNpcTrades().get(npcId);
            if (trades != null) {
                int totalPages = (int) Math.ceil((double) trades.size() / 5);
                int current = plugin.getPlayerShopPage().getOrDefault(player.getUniqueId(), 0);
                if (current < totalPages - 1) {
                    plugin.getPlayerShopPage().put(player.getUniqueId(), current + 1);
                    plugin.getGuiManager().openNpcShopGUI(player, npcId);
                }
            }
            return;
        }

        if (clickedSlot == 52) { // Close
            player.closeInventory();
            return;
        }

        // Handle trade execution - Slots 5, 14, 23, 32, 41
        if (clickedSlot % 9 == 5 && clickedSlot >= 5 && clickedSlot <= 41) {
            Map<Integer, String> tradeSlots = plugin.getPlayerTradeSlots().get(player.getUniqueId());

            if (tradeSlots != null && tradeSlots.containsKey(clickedSlot)) {
                String tradeId = tradeSlots.get(clickedSlot);
                plugin.getCurrentTradeId().put(player.getUniqueId(), tradeId);
                executeTrade(player, event.isShiftClick());
            }
        }
    }

    private void handleTradeEditClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
            return;
        }
        event.setCancelled(true);

        String npcId = plugin.getEditingNpc().get(player.getUniqueId());
        String tradeId = plugin.getEditingTradeId().get(player.getUniqueId());
        if (npcId == null || tradeId == null) return;

        TradeData trade = plugin.getNpcTrades().get(npcId).get(tradeId);
        if (trade == null) return;

        // Handle save button
        if (slot == plugin.getConfigManager().getGuiConfig().getInt("trade-edit.save-button.slot")) {
            saveTrade(player, event.getClickedInventory());
            return;
        }

        // Handle back button
        if (slot == plugin.getConfigManager().getGuiConfig().getInt("trade-edit.back-button.slot")) {
            plugin.getGuiManager().openNpcTradeManagementGUI(player, npcId);
            return;
        }

        // Handle currency buttons
        String inputType = null;
        if (slot == plugin.getConfigManager().getGuiConfig().getInt("trade-edit.required-money-slot")) {
            inputType = "required_money";
        } else if (slot == plugin.getConfigManager().getGuiConfig().getInt("trade-edit.required-points-slot")) {
            inputType = "required_points";
        } else if (slot == plugin.getConfigManager().getGuiConfig().getInt("trade-edit.reward-money-slot")) {
            inputType = "reward_money";
        } else if (slot == plugin.getConfigManager().getGuiConfig().getInt("trade-edit.reward-points-slot")) {
            inputType = "reward_points";
        }

        if (inputType != null) {
            if (event.getClick() == ClickType.RIGHT) {
                // Reset to 0
                switch (inputType) {
                    case "required_money":
                        trade.setRequiredMoney(0);
                        break;
                    case "required_points":
                        trade.setRequiredPoints(0);
                        break;
                    case "reward_money":
                        trade.setRewardMoney(0);
                        break;
                    case "reward_points":
                        trade.setRewardPoints(0);
                        break;
                }
                plugin.getGuiManager().openTradeEditGUI(player, npcId, tradeId);
            } else {
                // Save current items before requesting chat input
                saveCurrentItemsFromGUI(player, event.getClickedInventory(), trade);

                // Request input
                plugin.getChatInput().put(player.getUniqueId(), inputType);
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getMessage(
                        inputType.contains("money") ? "currency-enter-money-amount" : "currency-enter-points-amount"));
            }
            return;
        }

        // Handle item slots
        List<Integer> editableSlots = new ArrayList<>();
        editableSlots.addAll(plugin.getConfigManager().getGuiConfig().getIntegerList("trade-edit.required-item-slots"));
        editableSlots.addAll(plugin.getConfigManager().getGuiConfig().getIntegerList("trade-edit.reward-item-slots"));

        if (editableSlots.contains(slot)) {
            event.setCancelled(false);
        }
    }

    private void executeTrade(Player player, boolean isShiftClick) {
        String npcId = plugin.getCurrentTradeNpc().get(player.getUniqueId());
        String tradeId = plugin.getCurrentTradeId().get(player.getUniqueId());
        if (npcId == null || tradeId == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("error-trade-data-not-found"));
            player.closeInventory();
            return;
        }

        TradeData trade = plugin.getNpcTrades().get(npcId).get(tradeId);
        if (trade == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("error-trade-not-exist"));
            player.closeInventory();
            return;
        }

        int tradesToPerform = isShiftClick ? plugin.getTradeManager().calculateMaxTrades(player, trade) : 1;

        if (tradesToPerform == 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("trade-insufficient-items"));
            return;
        }

        if (plugin.getTradeManager().executeTrade(player, trade, tradesToPerform)) {
            // Send notification
            if (isShiftClick && tradesToPerform > 1) {
                player.sendMessage(plugin.getConfigManager().getMessage("trade-completed-multiple")
                        .replace("{times}", String.valueOf(tradesToPerform)));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("trade-completed"));
            }

            // Clear current trade ID immediately
            plugin.getCurrentTradeId().remove(player.getUniqueId());

            // Delay reopening GUI to ensure inventory is updated
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Check if player is still in shop GUI
                        String currentTitle = "";
                        try {
                            currentTitle = getInventoryTitle(player.getOpenInventory());
                        } catch (Exception e) {
                            // Ignore
                        }

                        String shopTitle = plugin.getConfigManager().translateColors(
                                plugin.getConfigManager().getGuiConfig().getString("npc-shop.title", "&1&lTrao Đổi"));

                        // Only reopen if player is still in shop GUI
                        if (currentTitle.equals(shopTitle)) {
                            plugin.getGuiManager().openNpcShopGUI(player, npcId);
                        }
                    }
                }
            }.runTaskLater(plugin, 2L);
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("trade-insufficient-items"));
        }
    }

    private String generateNextTradeId(Set<String> existingIds) {
        int nextId = 1;
        while (existingIds.contains(String.valueOf(nextId))) {
            nextId++;
        }
        return String.valueOf(nextId);
    }

    private void saveCurrentItemsFromGUI(Player player, Inventory gui, TradeData trade) {
        if (gui == null || gui.getSize() != 54) return;

        List<ItemStack> currentRequiredItems = new ArrayList<>();
        List<ItemStack> currentRewardItems = new ArrayList<>();

        // Get items from required slots
        List<Integer> requiredSlots = plugin.getConfigManager().getGuiConfig().getIntegerList("trade-edit.required-item-slots");
        for (int slot : requiredSlots) {
            if (slot < gui.getSize()) {
                ItemStack item = gui.getItem(slot);
                if (item != null && item.getType() != Material.AIR && !isPlaceholderItem(item)) {
                    currentRequiredItems.add(item.clone());
                }
            }
        }

        // Get items from reward slots
        List<Integer> rewardSlots = plugin.getConfigManager().getGuiConfig().getIntegerList("trade-edit.reward-item-slots");
        for (int slot : rewardSlots) {
            if (slot < gui.getSize()) {
                ItemStack item = gui.getItem(slot);
                if (item != null && item.getType() != Material.AIR && !isPlaceholderItem(item)) {
                    currentRewardItems.add(item.clone());
                }
            }
        }

        // Update trade data with current items
        trade.setRequiredItems(currentRequiredItems);
        trade.setRewardItems(currentRewardItems);
    }

    private boolean isPlaceholderItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return true;
        }

        // Check for common placeholder materials
        Material type = item.getType();
        if (type == Material.BLACK_STAINED_GLASS_PANE ||
                type == Material.GRAY_STAINED_GLASS_PANE ||
                type == Material.BLUE_STAINED_GLASS_PANE) {
            return true;
        }

        // Check for items with empty display names (decorative items)
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName().trim();
            if (displayName.equals("") || displayName.equals(" ")) {
                return true;
            }
        }

        // Check for gold/experience bottle heads used for currency display
        if (type == Material.PLAYER_HEAD && item.hasItemMeta()) {
            String displayName = item.getItemMeta().getDisplayName();
            if (displayName.contains("Tiền") || displayName.contains("Điểm") ||
                    displayName.contains("Money") || displayName.contains("Points")) {
                return true;
            }
        }

        return false;
    }

    private String getInventoryTitle(org.bukkit.inventory.InventoryView view) {
        try {
            Method getTitleMethod = view.getClass().getMethod("getTitle");
            getTitleMethod.setAccessible(true);
            return (String) getTitleMethod.invoke(view);
        } catch (Exception e) {
            return "";
        }
    }

    private void deleteTrade(Player player, String npcId, String tradeId) {
        Map<String, TradeData> npcTradeMap = plugin.getNpcTrades().get(npcId);
        if (npcTradeMap != null) {
            npcTradeMap.remove(tradeId);
            if (npcTradeMap.isEmpty()) {
                plugin.getNpcTrades().remove(npcId);
            }
            plugin.getTradeDataManager().saveTrades();
            player.sendMessage(plugin.getConfigManager().getMessage("trade-deleted"));
            plugin.getGuiManager().openNpcTradeManagementGUI(player, npcId);
        }
    }

    private void saveTrade(Player player, Inventory gui) {
        String npcId = plugin.getEditingNpc().get(player.getUniqueId());
        String tradeId = plugin.getEditingTradeId().get(player.getUniqueId());
        if (npcId == null || tradeId == null) return;

        TradeData trade = plugin.getNpcTrades().get(npcId).get(tradeId);
        trade.setRequiredItems(new ArrayList<>());
        trade.setRewardItems(new ArrayList<>());

        ItemStack placeholder = plugin.getGuiManager().createConfiguredItem("trade-edit.placeholder-item", "", "");

        // Collect required items
        List<Integer> requiredSlots = plugin.getConfigManager().getGuiConfig().getIntegerList("trade-edit.required-item-slots");
        for (int slot : requiredSlots) {
            ItemStack item = gui.getItem(slot);
            if (item != null && !item.isSimilar(placeholder) && !item.getType().isAir()) {
                trade.getRequiredItems().add(item.clone());
            }
        }

        // Collect reward items
        List<Integer> rewardSlots = plugin.getConfigManager().getGuiConfig().getIntegerList("trade-edit.reward-item-slots");
        for (int slot : rewardSlots) {
            ItemStack item = gui.getItem(slot);
            if (item != null && !item.isSimilar(placeholder) && !item.getType().isAir()) {
                trade.getRewardItems().add(item.clone());
            }
        }

        plugin.getTradeDataManager().saveTrades();
        player.sendMessage(plugin.getConfigManager().getMessage("trade-saved"));
        player.closeInventory();
        plugin.getEditingNpc().remove(player.getUniqueId());
        plugin.getEditingTradeId().remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        String closedTitle = getInventoryTitle(event.getView());
        String shopTitle = plugin.getConfigManager().translateColors(
                plugin.getConfigManager().getGuiConfig().getString("npc-shop.title", "&1&lTrao Đổi"));

        if (closedTitle.equals(shopTitle)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }

                    // Clean up player data when shop is closed
                    plugin.getCurrentTradeNpc().remove(playerUUID);
                    plugin.getCurrentTradeId().remove(playerUUID);
                    plugin.getPlayerShopPage().remove(playerUUID);
                    plugin.getPlayerTradeSlots().remove(playerUUID);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    private String getInventoryTitle(InventoryClickEvent event) {
        try {
            Object view = event.getView();
            Method getTitleMethod = view.getClass().getMethod("getTitle");
            getTitleMethod.setAccessible(true);
            return (String) getTitleMethod.invoke(view);
        } catch (Exception e) {
            return "";
        }
    }
}