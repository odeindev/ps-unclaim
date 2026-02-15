package org.odeindev.autounclaim

import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.World
import java.util.*

/**
 * Поиск и удаление регионов неактивных игроков
 */
class RegionPruner(private val plugin: AutoUnclaim) {

    data class PruneResult(
        val removedRegions: Int,
        val affectedPlayers: Set<String>
    )

    /**
     * Проверяет неактивных игроков и удаляет их регионы
     */
    fun pruneInactiveRegions(): PruneResult {
        val inactivePlayers = findInactivePlayers()

        if (inactivePlayers.isEmpty()) {
            plugin.logger.info("[AutoUnclaim] No inactive players found")
            return PruneResult(0, emptySet())
        }

        val removedCount = inactivePlayers
            .mapNotNull { it.uniqueId }
            .sumOf { uuid -> removePlayerRegions(uuid) }

        return PruneResult(
            removedRegions = removedCount,
            affectedPlayers = inactivePlayers.mapNotNull { it.name }.toSet()
        )
    }

    /**
     * Находит всех неактивных игроков
     * Исключает игроков, которые сейчас онлайн
     */
    private fun findInactivePlayers(): List<OfflinePlayer> {
        val thresholdMillis = System.currentTimeMillis() - plugin.inactiveMillis

        return Bukkit.getOfflinePlayers()
            .filter { player ->
                // Пропускаем онлайн игроков
                if (player.isOnline) {
                    return@filter false
                }

                // Проверяем, заходил ли игрок в период неактивности
                player.lastPlayed > 0 && player.lastPlayed < thresholdMillis
            }
            .also { players ->
                plugin.logger.info(
                    "[AutoUnclaim] Found ${players.size} inactive player(s) " +
                            "(not logged in > ${plugin.inactiveTimeDisplay})"
                )
            }
    }

    /**
     * Удаляет все регионы ProtectionStones, принадлежащие игроку
     */
    private fun removePlayerRegions(playerUUID: UUID): Int {
        var totalRemoved = 0

        Bukkit.getWorlds().forEach { world ->
            totalRemoved += removeRegionsInWorld(world, playerUUID)
        }

        return totalRemoved
    }

    /**
     * Удаляет регионы игрока в конкретном мире
     */
    private fun removeRegionsInWorld(world: World, playerUUID: UUID): Int {
        val regionManager = WorldGuard.getInstance()
            .platform
            .regionContainer
            .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world))
            ?: return 0

        // Находим регионы с префиксом "ps", принадлежащие игроку
        val regionsToRemove = regionManager.regions.values
            .filter { region ->
                region.isOwnedByPlayer(playerUUID) &&
                        region.id.startsWith("ps", ignoreCase = true)
            }

        regionsToRemove.forEach { region ->
            regionManager.removeRegion(region.id)
            plugin.logger.info("[AutoUnclaim] Removed region: ${region.id} (world: ${world.name})")
        }

        return regionsToRemove.size
    }

    /**
     * Проверяет, принадлежит ли регион конкретному игроку
     */
    private fun ProtectedRegion.isOwnedByPlayer(playerUUID: UUID): Boolean {
        return owners.contains(playerUUID)
    }
}