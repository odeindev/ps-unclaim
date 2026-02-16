package org.odeindev.autounclaim

import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.World
import java.util.*

/**
 * Информация о регионе для отчетности
 */
data class RegionInfo(
    val regionId: String,
    val worldName: String,
    val ownerName: String,
    val ownerUUID: UUID
)

/**
 * Поиск и удаление регионов неактивных игроков
 */
class RegionPruner(private val plugin: AutoUnclaim) {

    data class PruneResult(
        val removedRegions: Int,
        val affectedPlayers: Set<String>,
        val regionDetails: Map<String, List<RegionInfo>> = emptyMap() // world -> regions
    ) {
        /**
         * Получить общее количество регионов по всем мирам
         */
        fun getTotalRegionsByWorld(): Map<String, Int> {
            return regionDetails.mapValues { it.value.size }
        }
    }

    /**
     * Проверяет неактивных игроков и удаляет их регионы (или просто анализирует в dry-run режиме)
     *
     * @param mode Режим выполнения: REAL или DRY_RUN
     * @return Результат операции с детальной информацией
     */
    fun pruneInactiveRegions(mode: ExecutionMode = ExecutionMode.REAL): PruneResult {
        val inactivePlayers = findInactivePlayers()

        if (inactivePlayers.isEmpty()) {
            plugin.logger.info("[AutoUnclaim] No inactive players found")
            return PruneResult(0, emptySet(), emptyMap())
        }

        // Собираем информацию о всех регионах неактивных игроков
        val allRegionsInfo = mutableMapOf<String, MutableList<RegionInfo>>()
        var totalCount = 0

        inactivePlayers.forEach { player ->
            player.uniqueId?.let { uuid ->
                val playerRegions = collectPlayerRegions(uuid, player.name ?: "Unknown")

                playerRegions.forEach { (world, regions) ->
                    allRegionsInfo.getOrPut(world) { mutableListOf() }.addAll(regions)
                    totalCount += regions.size
                }
            }
        }

        // Если режим REAL - удаляем регионы
        if (mode.shouldDelete()) {
            allRegionsInfo.forEach { (worldName, regions) ->
                val world = Bukkit.getWorld(worldName)
                if (world != null) {
                    removeRegionsInWorld(world, regions)
                } else {
                    plugin.logger.warning("[AutoUnclaim] World '$worldName' not found, skipping...")
                }
            }
        }

        return PruneResult(
            removedRegions = totalCount,
            affectedPlayers = inactivePlayers.mapNotNull { it.name }.toSet(),
            regionDetails = allRegionsInfo
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
     * Собирает информацию о всех регионах игрока во всех мирах
     *
     * @return Map<WorldName, List<RegionInfo>>
     */
    private fun collectPlayerRegions(playerUUID: UUID, playerName: String): Map<String, List<RegionInfo>> {
        val regionsByWorld = mutableMapOf<String, MutableList<RegionInfo>>()

        Bukkit.getWorlds().forEach { world ->
            val regionsInWorld = findRegionsInWorld(world, playerUUID, playerName)
            if (regionsInWorld.isNotEmpty()) {
                regionsByWorld[world.name] = regionsInWorld.toMutableList()
            }
        }

        return regionsByWorld
    }

    /**
     * Находит регионы игрока в конкретном мире (без удаления)
     */
    private fun findRegionsInWorld(world: World, playerUUID: UUID, playerName: String): List<RegionInfo> {
        val regionManager = WorldGuard.getInstance()
            .platform
            .regionContainer
            .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world))
            ?: return emptyList()

        // Находим регионы с префиксом "ps", принадлежащие игроку
        return regionManager.regions.values
            .filter { region ->
                region.isOwnedByPlayer(playerUUID) &&
                        region.id.startsWith("ps", ignoreCase = true)
            }
            .map { region ->
                RegionInfo(
                    regionId = region.id,
                    worldName = world.name,
                    ownerName = playerName,
                    ownerUUID = playerUUID
                )
            }
    }

    /**
     * Удаляет указанные регионы в мире
     */
    private fun removeRegionsInWorld(world: World, regions: List<RegionInfo>) {
        val regionManager = WorldGuard.getInstance()
            .platform
            .regionContainer
            .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world))
            ?: return

        regions.forEach { regionInfo ->
            regionManager.removeRegion(regionInfo.regionId)
            plugin.logger.info(
                "[AutoUnclaim] Removed region: ${regionInfo.regionId} " +
                        "(owner: ${regionInfo.ownerName}, world: ${world.name})"
            )
        }
    }

    /**
     * Проверяет, принадлежит ли регион конкретному игроку
     */
    private fun ProtectedRegion.isOwnedByPlayer(playerUUID: UUID): Boolean {
        return owners.contains(playerUUID)
    }
}