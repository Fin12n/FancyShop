package org.quang1807.fancyshop.utils

import org.quang1807.fancyshop.config.ConfigManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.profile.PlayerProfile
import org.bukkit.profile.PlayerTextures
import java.net.URL
import java.util.*

object SkullUtils {

    const val GOLD_HEAD_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTQ2N2E3YjlkNzZiYTZkMGZlZDc0MzYwMjUzM2ZjOThjODdhZjBjNjBmODBmMzhkYTc3NGY3YTAxYTIwOTNmYSJ9fX0="
    const val POINT_HEAD_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWJkYTVmMzE5MzdiMmZmNzU1MjcxZDk3ZjAxYmU4NGQ1MmE0MDdiMzZjYTc3NDUxODU2MTYyYWM2Y2ZiYjM0ZiJ9fX0="

    fun createGoldHead(money: Double, points: Int, configManager: ConfigManager): ItemStack? {
        if (money <= 0 && points <= 0) return null

        val title: String
        val lore: List<String>

        if (money > 0) {
            title = configManager.getMessage("currency-money-title")
            lore = listOf(configManager.getMessage("currency-money-amount").replace("{amount}", String.format("%,.0f", money)))
        } else {
            title = configManager.getMessage("currency-points-title")
            lore = listOf(configManager.getMessage("currency-points-amount").replace("{amount}", points.toString()))
        }

        return createCustomHead(GOLD_HEAD_TEXTURE, title, lore, configManager)
    }

    fun createPointHead(points: Int, configManager: ConfigManager): ItemStack? {
        if (points <= 0) return null

        val title = configManager.getMessage("currency-points-title")
        val lore = listOf(configManager.getMessage("currency-points-amount").replace("{amount}", points.toString()))

        return createCustomHead(POINT_HEAD_TEXTURE, title, lore, configManager)
    }

    fun createCustomHead(textureValue: String, displayName: String, lore: List<String>, configManager: ConfigManager): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as? SkullMeta ?: return head

        meta.setDisplayName(configManager.translateColors(displayName))
        if (lore.isNotEmpty()) {
            val coloredLore = lore.map { configManager.translateColors(it) }
            meta.lore = coloredLore
        }

        applyTexture(meta, textureValue)
        head.itemMeta = meta
        return head
    }

    private fun applyTexture(meta: SkullMeta, base64: String) {
        try {
            // Try modern API first (1.18+)
            val profile = Bukkit.createPlayerProfile(UUID.randomUUID())

            val decodedJson = String(Base64.getDecoder().decode(base64))
            val urlStartIndex = decodedJson.indexOf("http")
            if (urlStartIndex == -1) return
            val urlEndIndex = decodedJson.indexOf("\"", urlStartIndex)
            val textureUrl = decodedJson.substring(urlStartIndex, urlEndIndex)

            // 👇 Không tạo val textures để tránh lỗi reassigned
            profile.textures.skin = URL(textureUrl)

            meta.ownerProfile = profile
        } catch (e: Exception) {
            // Fallback to legacy method using reflection
            try {
                val gameProfileClass = Class.forName("com.mojang.authlib.GameProfile")
                val gameProfileConstructor = gameProfileClass.getConstructor(UUID::class.java, String::class.java)
                val gameProfile = gameProfileConstructor.newInstance(UUID.randomUUID(), null)

                val getPropertiesMethod = gameProfile.javaClass.getMethod("getProperties")
                val propertyMap = getPropertiesMethod.invoke(gameProfile)

                val propertyClass = Class.forName("com.mojang.authlib.properties.Property")
                val propertyConstructor = propertyClass.getConstructor(String::class.java, String::class.java)
                val textureProperty = propertyConstructor.newInstance("textures", base64)

                val putMethod = propertyMap.javaClass.getMethod("put", Any::class.java, Any::class.java)
                putMethod.invoke(propertyMap, "textures", textureProperty)

                val profileField = meta.javaClass.getDeclaredField("profile")
                profileField.isAccessible = true
                profileField.set(meta, gameProfile)
            } catch (reflectionException: Exception) {
                // If both methods fail, just use a regular player head
            }
        }
    }
}