package org.quang1807.fancyshop.data

import org.bukkit.inventory.ItemStack

data class TradeData(
    var requiredItems: MutableList<ItemStack> = mutableListOf(),
    var requiredMoney: Double = 0.0,
    var requiredPoints: Int = 0,
    var rewardItems: MutableList<ItemStack> = mutableListOf(),
    var rewardMoney: Double = 0.0,
    var rewardPoints: Int = 0
)