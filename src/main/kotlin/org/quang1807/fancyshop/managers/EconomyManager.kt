package org.quang1807.fancyshop.managers

import org.quang1807.fancyshop.FancyShop
import net.milkbowl.vault.economy.Economy
import org.black_ixx.playerpoints.PlayerPointsAPI
import org.bukkit.plugin.RegisteredServiceProvider

class EconomyManager(private val plugin: FancyShop) {

    var economy: Economy? = null
        private set
    var playerPointsAPI: PlayerPointsAPI? = null
        private set

    fun setupEconomy(): Boolean {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            return false
        }

        val rsp: RegisteredServiceProvider<Economy>? = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            return false
        }

        economy = rsp.provider
        return economy != null
    }

    fun setupPlayerPoints() {
        val plugin = this.plugin.server.pluginManager.getPlugin("PlayerPoints")
        if (plugin != null && plugin is org.black_ixx.playerpoints.PlayerPoints) {
            try {
                playerPointsAPI = plugin.api
                this.plugin.logger.info("PlayerPoints integration enabled!")
            } catch (e: Exception) {
                this.plugin.logger.warning("Failed to hook into PlayerPoints: ${e.message}")
            }
        }
    }
}