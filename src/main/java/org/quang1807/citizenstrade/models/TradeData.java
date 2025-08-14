package org.quang1807.citizenstrade.models;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TradeData {
    private List<ItemStack> requiredItems;
    private double requiredMoney;
    private int requiredPoints;
    private List<ItemStack> rewardItems;
    private double rewardMoney;
    private int rewardPoints;

    public TradeData() {
        this.requiredMoney = 0;
        this.requiredPoints = 0;
        this.rewardMoney = 0;
        this.rewardPoints = 0;
        this.requiredItems = new ArrayList<>();
        this.rewardItems = new ArrayList<>();
    }

    // Required Items
    public List<ItemStack> getRequiredItems() {
        return requiredItems;
    }

    public void setRequiredItems(List<ItemStack> requiredItems) {
        this.requiredItems = requiredItems != null ? requiredItems : new ArrayList<>();
    }

    // Required Money
    public double getRequiredMoney() {
        return requiredMoney;
    }

    public void setRequiredMoney(double requiredMoney) {
        this.requiredMoney = requiredMoney;
    }

    // Required Points
    public int getRequiredPoints() {
        return requiredPoints;
    }

    public void setRequiredPoints(int requiredPoints) {
        this.requiredPoints = requiredPoints;
    }

    // Reward Items
    public List<ItemStack> getRewardItems() {
        return rewardItems;
    }

    public void setRewardItems(List<ItemStack> rewardItems) {
        this.rewardItems = rewardItems != null ? rewardItems : new ArrayList<>();
    }

    // Reward Money
    public double getRewardMoney() {
        return rewardMoney;
    }

    public void setRewardMoney(double rewardMoney) {
        this.rewardMoney = rewardMoney;
    }

    // Reward Points
    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    // Utility methods
    public boolean hasRequirements() {
        return (requiredItems != null && !requiredItems.isEmpty()) ||
                requiredMoney > 0 ||
                requiredPoints > 0;
    }

    public boolean hasRewards() {
        return (rewardItems != null && !rewardItems.isEmpty()) ||
                rewardMoney > 0 ||
                rewardPoints > 0;
    }

    public boolean isValid() {
        return hasRequirements() && hasRewards();
    }
}