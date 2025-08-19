package org.quang1807.citizenstrade.gui;

import org.quang1807.citizenstrade.CitizenShop;
import org.quang1807.citizenstrade.models.TradeData;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.*;

public class GuiManager {
    private final CitizenShop plugin;
    private final int TRADES_PER_PAGE = 5;

    // Custom head textures
    private static final String GOLD_HEAD_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTQ2N2E3YjlkNzZiYTZkMGZlZDc0MzYwMjUzM2ZjOThjODdhZjBjNjBmODBmMzhkYTc3NGY3YTAxYTIwOTNmYSJ9fX0=";
    private static final String POINT_HEAD_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWJkYTVmMzE5MzdiMmZmNzU1MjcxZDk3ZjAxYmU4NGQ1MmE0MDdiMzZjYTc3NDUxODU2MTYyYWM2Y2ZiYjM0ZiJ9fX0=";

    public GuiManager(CitizenShop plugin) {
        this.plugin = plugin;
    }

    public void openNpcSelectionGUI(Player player) {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        if (registry == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("error-loading-npcs"));
            return;
        }

        List<NPC> allNpcs = new ArrayList<>();
        for (NPC npc : registry) {
            if (npc != null && npc.getName() != null) {
                allNpcs.add(npc);
            }
        }

        if (allNpcs.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-npcs-found"));
            return;
        }

        String title = plugin.getConfigManager().translateColors(
                plugin.getConfigManager().getGuiConfig().getString("npc-selection.title", "&9Chọn NPC để chỉnh sửa"));
        int size = plugin.getConfigManager().getGuiConfig().getInt("npc-selection.size", 54);
        Inventory gui = Bukkit.createInventory(null, size, title);

        fillDecorative(gui, "npc-selection");

        List<Integer> npcSlots = plugin.getConfigManager().getGuiConfig().getIntegerList("npc-selection.npc-slots");
        if (npcSlots.isEmpty()) {
            npcSlots = Arrays.asList(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
        }

        int currentPage = plugin.getPlayerCurrentPage().getOrDefault(player.getUniqueId(), 0);
        int itemsPerPage = npcSlots.size();
        int totalPages = (int) Math.ceil((double) allNpcs.size() / itemsPerPage);
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allNpcs.size());

        List<NPC> npcsOnPage = allNpcs.subList(startIndex, endIndex);

        int npcIndex = 0;
        for (NPC npc : npcsOnPage) {
            if (npcIndex >= npcSlots.size()) break;

            String npcId = String.valueOf(npc.getId());
            ItemStack item = createConfiguredItem("npc-selection.npc-item", npc.getName(), npcId);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                for (String line : plugin.getConfigManager().getGuiConfig().getStringList("npc-selection.npc-item.lore")) {
                    line = line.replace("{npc_id}", npcId);
                    line = line.replace("{npc_name}", npc.getName());

                    Map<String, TradeData> npcTradeMap = plugin.getNpcTrades().get(npcId);
                    if (npcTradeMap != null && !npcTradeMap.isEmpty()) {
                        line = line.replace("{status}", plugin.getConfigManager().getGuiConfig().getString("npc-selection.status.configured", "&a✓ Đã cấu hình giao dịch"));
                        line = line.replace("{trade_count}", String.valueOf(npcTradeMap.size()));
                    } else {
                        line = line.replace("{status}", plugin.getConfigManager().getGuiConfig().getString("npc-selection.status.not-configured", "&c✗ Chưa cấu hình giao dịch"));
                        line = line.replace("{trade_count}", "0");
                    }
                    lore.add(plugin.getConfigManager().translateColors(line));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(npcSlots.get(npcIndex++), item);
        }

        // Pagination buttons
        if (currentPage > 0) {
            int prevButtonSlot = plugin.getConfigManager().getGuiConfig().getInt("npc-selection.pagination.prev-button.slot", 48);
            ItemStack prevButton = createConfiguredItem("npc-selection.pagination.prev-button", "", "");
            gui.setItem(prevButtonSlot, prevButton);
        }

        if (currentPage < totalPages - 1) {
            int nextButtonSlot = plugin.getConfigManager().getGuiConfig().getInt("npc-selection.pagination.next-button.slot", 50);
            ItemStack nextButton = createConfiguredItem("npc-selection.pagination.next-button", "", "");
            gui.setItem(nextButtonSlot, nextButton);
        }

        // Page info
        int pageInfoSlot = plugin.getConfigManager().getGuiConfig().getInt("npc-selection.pagination.page-info-item.slot", 49);
        ItemStack pageInfoItem = createConfiguredItem("npc-selection.pagination.page-info-item", "", "");
        ItemMeta pageInfoMeta = pageInfoItem.getItemMeta();
        if (pageInfoMeta != null) {
            String displayName = plugin.getConfigManager().getGuiConfig().getString("npc-selection.pagination.page-info-item.name", "&e&lTrang {current_page}/{total_pages}");
            displayName = displayName.replace("{current_page}", String.valueOf(currentPage + 1));
            displayName = displayName.replace("{total_pages}", String.valueOf(totalPages));
            pageInfoMeta.setDisplayName(plugin.getConfigManager().translateColors(displayName));

            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfigManager().getGuiConfig().getStringList("npc-selection.pagination.page-info-item.lore")) {
                line = line.replace("{total_npcs}", String.valueOf(allNpcs.size()));
                lore.add(plugin.getConfigManager().translateColors(line));
            }
            pageInfoMeta.setLore(lore);
            pageInfoItem.setItemMeta(pageInfoMeta);
        }
        gui.setItem(pageInfoSlot, pageInfoItem);

        player.openInventory(gui);
    }

    public void openNpcTradeManagementGUI(Player player, String npcId) {
        String title = plugin.getConfigManager().translateColors(
                plugin.getConfigManager().getGuiConfig().getString("npc-trade-management.title", "&6Quản lý giao dịch NPC").replace("{npc_id}", npcId));
        int size = plugin.getConfigManager().getGuiConfig().getInt("npc-trade-management.size", 54);
        Inventory gui = Bukkit.createInventory(null, size, title);

        fillDecorative(gui, "npc-trade-management");

        Map<String, TradeData> npcTradeMap = plugin.getNpcTrades().getOrDefault(npcId, new HashMap<>());

        // Trade display slots
        List<Integer> tradeSlots = Arrays.asList(2, 5, 11, 14, 20, 23, 29, 32, 38, 41, 47, 50);

        int slotIndex = 0;
        for (Map.Entry<String, TradeData> entry : npcTradeMap.entrySet()) {
            if (slotIndex >= tradeSlots.size()) break;

            String tradeId = entry.getKey();
            TradeData trade = entry.getValue();

            ItemStack displayItem = createTradeDisplayItem(trade);
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add(plugin.getConfigManager().translateColors("&7Trade ID: &f" + tradeId));
                lore.add(plugin.getConfigManager().translateColors("&e&lClick trái: &7Chỉnh sửa giao dịch"));
                lore.add(plugin.getConfigManager().translateColors("&c&lClick phải: &7Xóa giao dịch"));
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }

            gui.setItem(tradeSlots.get(slotIndex++), displayItem);
        }

        // Add button
        if (slotIndex < tradeSlots.size()) {
            int addButtonSlot = plugin.getConfigManager().getGuiConfig().getInt("npc-trade-management.add-button.slot", 40);
            ItemStack addButton = createConfiguredItem("npc-trade-management.add-button", "", "");
            gui.setItem(addButtonSlot, addButton);
        }

        // Back button
        int backSlot = plugin.getConfigManager().getGuiConfig().getInt("npc-trade-management.back-button.slot", 4);
        ItemStack backButton = createConfiguredItem("npc-trade-management.back-button", "", "");
        gui.setItem(backSlot, backButton);

        plugin.getEditingNpc().put(player.getUniqueId(), npcId);
        player.openInventory(gui);
    }

    public void openTradeEditGUI(Player player, String npcId, String tradeId) {
        String title = plugin.getConfigManager().translateColors(
                plugin.getConfigManager().getGuiConfig().getString("trade-edit.title", "&2Chỉnh sửa giao dịch"));
        Inventory gui = Bukkit.createInventory(null, 54, title);
        fillDecorative(gui, "trade-edit");

        TradeData trade = plugin.getNpcTrades()
                .computeIfAbsent(npcId, k -> new HashMap<>())
                .computeIfAbsent(tradeId, k -> new TradeData());

        // Load required items
        List<Integer> requiredItemSlots = plugin.getConfigManager().getGuiConfig().getIntegerList("trade-edit.required-item-slots");
        if (trade.getRequiredItems() != null) {
            for (int i = 0; i < Math.min(trade.getRequiredItems().size(), requiredItemSlots.size()); i++) {
                gui.setItem(requiredItemSlots.get(i), trade.getRequiredItems().get(i));
            }
        }

        // Load reward items
        List<Integer> rewardSlots = plugin.getConfigManager().getGuiConfig().getIntegerList("trade-edit.reward-item-slots");
        if (trade.getRewardItems() != null) {
            for (int i = 0; i < Math.min(trade.getRewardItems().size(), rewardSlots.size()); i++) {
                gui.setItem(rewardSlots.get(i), trade.getRewardItems().get(i));
            }
        }

        // Money and points heads
        ItemStack reqMoneyHead = createGoldHead(trade.getRequiredMoney(), 0);
        gui.setItem(plugin.getConfigManager().getGuiConfig().getInt("trade-edit.required-money-slot"),
                reqMoneyHead != null ? reqMoneyHead : createConfiguredItem("trade-edit.add-money-item", "", ""));

        ItemStack reqPointHead = createPointHead(trade.getRequiredPoints());
        gui.setItem(plugin.getConfigManager().getGuiConfig().getInt("trade-edit.required-points-slot"),
                reqPointHead != null ? reqPointHead : createConfiguredItem("trade-edit.add-points-item", "", ""));

        ItemStack rewardMoneyHead = createGoldHead(trade.getRewardMoney(), 0);
        gui.setItem(plugin.getConfigManager().getGuiConfig().getInt("trade-edit.reward-money-slot"),
                rewardMoneyHead != null ? rewardMoneyHead : createConfiguredItem("trade-edit.add-money-item", "", ""));

        ItemStack rewardPointHead = createPointHead(trade.getRewardPoints());
        gui.setItem(plugin.getConfigManager().getGuiConfig().getInt("trade-edit.reward-points-slot"),
                rewardPointHead != null ? rewardPointHead : createConfiguredItem("trade-edit.add-points-item", "", ""));

        // Control buttons
        gui.setItem(plugin.getConfigManager().getGuiConfig().getInt("trade-edit.save-button.slot"),
                createConfiguredItem("trade-edit.save-button", "", ""));
        gui.setItem(plugin.getConfigManager().getGuiConfig().getInt("trade-edit.back-button.slot"),
                createConfiguredItem("trade-edit.back-button", "", ""));

        plugin.getEditingNpc().put(player.getUniqueId(), npcId);
        plugin.getEditingTradeId().put(player.getUniqueId(), tradeId);
        player.openInventory(gui);
    }

    public void openNpcShopGUI(Player player, String npcId) {
        Map<String, TradeData> npcTradeMap = plugin.getNpcTrades().get(npcId);
        if (npcTradeMap == null || npcTradeMap.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("shop-no-trades"));
            return;
        }

        String title = plugin.getConfigManager().translateColors(
                plugin.getConfigManager().getGuiConfig().getString("npc-shop.title", "&1&lTrao Đổi"));
        Inventory gui = Bukkit.createInventory(player, 54, title);
        fillDecorative(gui, "npc-shop");

        List<String> sortedTradeIds = new ArrayList<>(npcTradeMap.keySet());
        Collections.sort(sortedTradeIds);

        int currentPage = plugin.getPlayerShopPage().getOrDefault(player.getUniqueId(), 0);
        int totalTrades = sortedTradeIds.size();
        int totalPages = (int) Math.ceil((double) totalTrades / TRADES_PER_PAGE);

        if (currentPage >= totalPages && totalPages > 0) {
            currentPage = totalPages - 1;
            plugin.getPlayerShopPage().put(player.getUniqueId(), currentPage);
        }

        int startIndex = currentPage * TRADES_PER_PAGE;
        int endIndex = Math.min(startIndex + TRADES_PER_PAGE, totalTrades);

        // QUAN TRỌNG: Luôn tạo lại tradeSlotsForPlayer mỗi lần mở GUI
        Map<Integer, String> tradeSlotsForPlayer = new HashMap<>();
        int lastDrawnRow = -1;

        for (int i = startIndex; i < endIndex; i++) {
            String tradeId = sortedTradeIds.get(i);
            int row = i % TRADES_PER_PAGE;
            drawTradeRow(gui, player, npcTradeMap.get(tradeId), row);

            // Lưu mapping slot -> tradeId
            int tradeSlot = 5 + (row * 9);
            tradeSlotsForPlayer.put(tradeSlot, tradeId);
            lastDrawnRow = row;
        }

        // QUAN TRỌNG: Cập nhật lại playerTradeSlots
        plugin.getPlayerTradeSlots().put(player.getUniqueId(), tradeSlotsForPlayer);

        // Fill empty slots
        ItemStack filler = createConfiguredItem("npc-shop.empty-trade-slot", "", "");
        for (int row = lastDrawnRow + 1; row < TRADES_PER_PAGE; row++) {
            int startSlot = row * 9;
            gui.setItem(startSlot + 2, filler);
            gui.setItem(startSlot + 3, filler);
            gui.setItem(startSlot + 4, filler);
            gui.setItem(startSlot + 5, filler);
        }

        drawControlPanel(gui, player, currentPage, totalPages);

        // Cập nhật currentTradeNpc
        plugin.getCurrentTradeNpc().put(player.getUniqueId(), npcId);
        player.openInventory(gui);
    }

    private void drawTradeRow(Inventory gui, Player player, TradeData trade, int row) {
        int startSlot = row * 9;

        // Required items display
        List<ItemStack> required = new ArrayList<>();
        if (trade.getRequiredItems() != null) {
            trade.getRequiredItems().stream()
                    .filter(i -> i != null && i.getType() != Material.AIR)
                    .forEach(required::add);
        }

        ItemStack reqMoneyHead = createGoldHead(trade.getRequiredMoney(), 0);
        if (reqMoneyHead != null) {
            required.add(reqMoneyHead);
        }

        ItemStack reqPointHead = createPointHead(trade.getRequiredPoints());
        if (reqPointHead != null) {
            required.add(reqPointHead);
        }

        if (!required.isEmpty()) gui.setItem(startSlot + 2, required.get(0));
        if (required.size() > 1) gui.setItem(startSlot + 3, required.get(1));

        // Arrow
        ItemStack arrow = createConfiguredItem("npc-shop.arrow-item", "", "");
        gui.setItem(startSlot + 4, arrow);

        // Reward display
        ItemStack rewardItem = createTradeDisplayItem(trade);
        gui.setItem(startSlot + 5, rewardItem);
    }

    private void drawControlPanel(Inventory gui, Player player, int currentPage, int totalPages) {
        int lastColumn = 7;

        // Navigation buttons
        if (currentPage > 0) {
            gui.setItem(lastColumn, createConfiguredItem("npc-shop.control-panel.prev-page", "", ""));
        } else {
            gui.setItem(lastColumn, createConfiguredItem("npc-shop.control-panel.placeholder", "", ""));
        }

        // Page info
        ItemStack pageInfo = createConfiguredItem("npc-shop.control-panel.page-info", "", "");
        ItemMeta pageInfoMeta = pageInfo.getItemMeta();
        if (pageInfoMeta != null) {
            String name = pageInfoMeta.getDisplayName()
                    .replace("{current_page}", String.valueOf(currentPage + 1))
                    .replace("{total_pages}", String.valueOf(totalPages == 0 ? 1 : totalPages));
            pageInfoMeta.setDisplayName(plugin.getConfigManager().translateColors(name));
            pageInfo.setItemMeta(pageInfoMeta);
        }
        gui.setItem(lastColumn + 9, pageInfo);

        if (currentPage < totalPages - 1) {
            gui.setItem(lastColumn + 18, createConfiguredItem("npc-shop.control-panel.next-page", "", ""));
        } else {
            gui.setItem(lastColumn + 18, createConfiguredItem("npc-shop.control-panel.placeholder", "", ""));
        }

        // Player info
        ItemStack playerInfo = createConfiguredItem("npc-shop.control-panel.player-info", player.getName(), "");
        ItemMeta playerInfoMeta = playerInfo.getItemMeta();
        if (playerInfoMeta instanceof SkullMeta) {
            ((SkullMeta) playerInfoMeta).setOwningPlayer(player);
        }
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(plugin.getConfigManager().translateColors(
                plugin.getConfigManager().getMessage("shop-player-money")
                        .replace("{amount}", String.format("%,.0f", plugin.getEconomyManager().getBalance(player)))));
        if (plugin.getEconomyManager().isPlayerPointsEnabled()) {
            lore.add(plugin.getConfigManager().translateColors(
                    plugin.getConfigManager().getMessage("shop-player-points")
                            .replace("{amount}", String.format("%,d", plugin.getEconomyManager().getPoints(player)))));
        }
        playerInfoMeta.setLore(lore);
        playerInfo.setItemMeta(playerInfoMeta);
        gui.setItem(lastColumn + 27, playerInfo);

        // Help and close buttons
        gui.setItem(lastColumn + 36, createConfiguredItem("npc-shop.control-panel.help", "", ""));
        gui.setItem(lastColumn + 45, createConfiguredItem("npc-shop.control-panel.close", "", ""));
    }

    private ItemStack createTradeDisplayItem(TradeData trade) {
        // Try to use first reward item as display
        if (trade.getRewardItems() != null && !trade.getRewardItems().isEmpty()) {
            ItemStack displayItem = null;
            for (ItemStack item : trade.getRewardItems()) {
                if (item != null && !item.getType().isAir()) {
                    displayItem = item.clone();
                    break;
                }
            }

            if (displayItem != null) {
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add(" ");

                    int otherItemCount = -1;
                    for (ItemStack item : trade.getRewardItems()) {
                        if (item != null && !item.getType().isAir()) {
                            otherItemCount++;
                        }
                    }

                    if (trade.getRewardMoney() > 0) {
                        lore.add(plugin.getConfigManager().getMessage("shop-extra-reward-money")
                                .replace("{amount}", String.format("%,.0f", trade.getRewardMoney())));
                    }
                    if (trade.getRewardPoints() > 0) {
                        lore.add(plugin.getConfigManager().getMessage("shop-extra-reward-points")
                                .replace("{amount}", String.format("%,d", trade.getRewardPoints())));
                    }
                    if (otherItemCount > 0) {
                        lore.add(plugin.getConfigManager().getMessage("shop-extra-reward-items")
                                .replace("{count}", String.valueOf(otherItemCount)));
                    }

                    meta.setLore(lore);
                    displayItem.setItemMeta(meta);
                }
                return displayItem;
            }
        }

        // Fallback to money or points display
        if (trade.getRewardMoney() > 0) {
            String title = plugin.getConfigManager().getMessage("shop-reward-money-title")
                    .replace("{amount}", String.format("%,.0f", trade.getRewardMoney()));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().getMessage("shop-reward-money-lore")
                    .replace("{amount}", String.format("%,.0f", trade.getRewardMoney())));
            if (trade.getRewardPoints() > 0) {
                lore.add(plugin.getConfigManager().getMessage("shop-extra-reward-points")
                        .replace("{amount}", String.format("%,d", trade.getRewardPoints())));
            }
            return createCustomHead(GOLD_HEAD_TEXTURE, title, lore);
        }

        if (trade.getRewardPoints() > 0) {
            String title = plugin.getConfigManager().getMessage("shop-reward-points-title")
                    .replace("{amount}", String.format("%,d", trade.getRewardPoints()));
            List<String> lore = Collections.singletonList(
                    plugin.getConfigManager().getMessage("shop-reward-points-lore")
                            .replace("{amount}", String.format("%,d", trade.getRewardPoints())));
            return createCustomHead(POINT_HEAD_TEXTURE, title, lore);
        }

        // Final fallback
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        if (barrierMeta != null) {
            barrierMeta.setDisplayName(plugin.getConfigManager().translateColors("&c&lGiao dịch trống"));
            barrierMeta.setLore(Collections.singletonList(
                    plugin.getConfigManager().translateColors("&7Giao dịch này chưa được cấu hình")));
            barrier.setItemMeta(barrierMeta);
        }
        return barrier;
    }

    public ItemStack createGoldHead(double money, int points) {
        String title;
        List<String> lore;

        if (money > 0) {
            title = plugin.getConfigManager().getMessage("currency-money-title");
            lore = Collections.singletonList(
                    plugin.getConfigManager().getMessage("currency-money-amount")
                            .replace("{amount}", String.format("%,.0f", money)));
        } else if (points > 0) {
            title = plugin.getConfigManager().getMessage("currency-points-title");
            lore = Collections.singletonList(
                    plugin.getConfigManager().getMessage("currency-points-amount")
                            .replace("{amount}", String.valueOf(points)));
        } else {
            return null;
        }

        return createCustomHead(GOLD_HEAD_TEXTURE, title, lore);
    }

    public ItemStack createPointHead(int points) {
        if (points <= 0) {
            return null;
        }
        String title = plugin.getConfigManager().getMessage("currency-points-title");
        List<String> lore = Collections.singletonList(
                plugin.getConfigManager().getMessage("currency-points-amount")
                        .replace("{amount}", String.valueOf(points)));

        return createCustomHead(POINT_HEAD_TEXTURE, title, lore);
    }

    private ItemStack createCustomHead(String textureValue, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        meta.setDisplayName(plugin.getConfigManager().translateColors(displayName));
        if (lore != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(plugin.getConfigManager().translateColors(line));
            }
            meta.setLore(coloredLore);
        }

        applyTexture(meta, textureValue);
        head.setItemMeta(meta);
        return head;
    }

    private void applyTexture(SkullMeta meta, String base64) {
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            String decodedJson = new String(Base64.getDecoder().decode(base64));

            int urlStartIndex = decodedJson.indexOf("http");
            if (urlStartIndex == -1) {
                plugin.getLogger().warning("Could not find texture URL in base64 data.");
                return;
            }
            int urlEndIndex = decodedJson.indexOf("\"", urlStartIndex);
            String textureUrl = decodedJson.substring(urlStartIndex, urlEndIndex);

            textures.setSkin(new URL(textureUrl));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply custom texture to skull: " + e.getMessage());
        }
    }

    private void fillDecorative(Inventory gui, String section) {
        ConfigurationSection decorSection = plugin.getConfigManager().getGuiConfig().getConfigurationSection(section + ".decorative");
        if (decorSection == null) return;

        for (String key : decorSection.getKeys(false)) {
            ConfigurationSection itemSection = decorSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            List<Integer> slots = itemSection.getIntegerList("slots");
            ItemStack item = createConfiguredItem(section + ".decorative." + key, "", "");
            for (int slot : slots) {
                if (slot >= 0 && slot < gui.getSize()) {
                    gui.setItem(slot, item);
                }
            }
        }
    }

    public ItemStack createConfiguredItem(String path, String npcName, String npcId) {
        ConfigurationSection section = plugin.getConfigManager().getGuiConfig().getConfigurationSection(path);
        if (section == null) {
            return new ItemStack(Material.STONE);
        }

        String materialName = section.getString("material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = section.getString("name", "");
            displayName = displayName.replace("{npc_name}", npcName).replace("{npc_id}", npcId);
            meta.setDisplayName(plugin.getConfigManager().translateColors(displayName));

            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                line = line.replace("{npc_name}", npcName).replace("{npc_id}", npcId);
                lore.add(plugin.getConfigManager().translateColors(line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}