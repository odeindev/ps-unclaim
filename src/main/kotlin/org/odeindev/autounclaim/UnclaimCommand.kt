package org.odeindev.autounclaim

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

/**
 * Обработчик команды /autounclaim
 */
class UnclaimCommand(private val plugin: AutoUnclaim) : CommandExecutor {

    /**
     * Обработка выполнения команды
     */
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        // Проверка прав доступа
        if (!sender.hasPermission("autounclaim.use")) {
            sender.sendMessage("§c[AutoUnclaim] You don't have permission to use this command")
            return true
        }

        // Направление к соответствующему обработчику
        when (args.getOrNull(0)?.lowercase()) {
            "run", "start" -> handleRun(sender, args)
            "reload" -> handleReload(sender)
            else -> handleHelp(sender)
        }

        return true
    }

    /**
     * Запуск проверки и удаление регионов (или dry-run)
     */
    private fun handleRun(sender: CommandSender, args: Array<out String>) {
        // Проверяем наличие флага --dry
        val isDryRun = args.any { it.equals("--dry", ignoreCase = true) || it.equals("-d", ignoreCase = true) }
        val mode = if (isDryRun) ExecutionMode.DRY_RUN else ExecutionMode.REAL

        val modeText = if (mode.isDryRun()) " §e[DRY-RUN MODE]" else ""
        sender.sendMessage("§e[AutoUnclaim]$modeText Starting inactive player check...")

        // Запускаем асинхронно, чтобы не блокировать главный поток
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val pruner = RegionPruner(plugin)
            val result = pruner.pruneInactiveRegions(mode)

            // Возвращаемся в главный поток для отправки сообщения
            plugin.server.scheduler.runTask(plugin, Runnable {
                if (mode.isDryRun()) {
                    sendDryRunReport(sender, result)
                } else {
                    sendRealRunReport(sender, result)
                }

                if (result.affectedPlayers.isNotEmpty()) {
                    plugin.logger.info("[AutoUnclaim] Affected players: ${result.affectedPlayers.joinToString()}")
                }
            })
        })
    }

    /**
     * Отправка отчета для dry-run режима
     */
    private fun sendDryRunReport(sender: CommandSender, result: RegionPruner.PruneResult) {
        sender.sendMessage("§6§l[AutoUnclaim] DRY-RUN REPORT")
        sender.sendMessage("§7§m-------------------------------------")

        if (result.removedRegions == 0) {
            sender.sendMessage("§a✓ No regions would be removed")
            sender.sendMessage("§7All players are active!")
            sender.sendMessage("§7§m-------------------------------------")
            return
        }

        sender.sendMessage("§e⚠ Regions that WOULD BE removed: §f${result.removedRegions}")
        sender.sendMessage("§e⚠ Players that WOULD BE affected: §f${result.affectedPlayers.size}")
        sender.sendMessage("")
        sender.sendMessage("§6Breakdown by world:")

        val regionsByWorld = result.getTotalRegionsByWorld()
        regionsByWorld.forEach { (world, count) ->
            sender.sendMessage("  §7• §f$world§7: §e$count region(s)")
        }

        sender.sendMessage("")
        sender.sendMessage("§6Affected players:")
        val playersList = result.affectedPlayers.take(10).joinToString("§7, §f")
        sender.sendMessage("  §f$playersList")

        if (result.affectedPlayers.size > 10) {
            sender.sendMessage("  §7... and ${result.affectedPlayers.size - 10} more")
        }

        sender.sendMessage("")
        sender.sendMessage("§6Region details (sample):")

        // Показываем до 5 регионов для примера
        val sampleRegions = result.regionDetails.values.flatten().take(5)
        sampleRegions.forEach { region ->
            sender.sendMessage("  §7• §f${region.regionId} §7(${region.worldName}, owner: ${region.ownerName})")
        }

        if (result.removedRegions > 5) {
            sender.sendMessage("  §7... and ${result.removedRegions - 5} more regions")
        }

        sender.sendMessage("")
        sender.sendMessage("§c⚠ This was a DRY-RUN. No regions were actually removed.")
        sender.sendMessage("§7To actually remove regions, run: §f/autounclaim run")
        sender.sendMessage("§7§m-------------------------------------")
    }

    /**
     * Отправка отчета для реального режима удаления
     */
    private fun sendRealRunReport(sender: CommandSender, result: RegionPruner.PruneResult) {
        sender.sendMessage("§a[AutoUnclaim] Check completed!")
        sender.sendMessage("§7§m-------------------------------------")

        if (result.removedRegions == 0) {
            sender.sendMessage("§a✓ No regions removed")
            sender.sendMessage("§7All players are active!")
        } else {
            sender.sendMessage("§7Removed regions: §f${result.removedRegions}")
            sender.sendMessage("§7Affected players: §f${result.affectedPlayers.size}")

            val regionsByWorld = result.getTotalRegionsByWorld()
            if (regionsByWorld.isNotEmpty()) {
                sender.sendMessage("")
                sender.sendMessage("§7Breakdown by world:")
                regionsByWorld.forEach { (world, count) ->
                    sender.sendMessage("  §7• §f$world§7: §a$count region(s)")
                }
            }
        }

        sender.sendMessage("§7§m-------------------------------------")
    }

    /**
     * Перезагрузка конфигурации
     */
    private fun handleReload(sender: CommandSender) {
        plugin.reloadConfiguration()
        sender.sendMessage("§a[AutoUnclaim] Config reloaded! Period: ${plugin.inactiveTimeDisplay}")

        if (plugin.autoRunEnabled) {
            sender.sendMessage("§7Auto-run: §aEnabled §7(every ${plugin.autoRunIntervalMinutes} min)")
        } else {
            sender.sendMessage("§7Auto-run: §cDisabled")
        }
    }

    /**
     * Показать справку по командам
     */
    private fun handleHelp(sender: CommandSender) {
        sender.sendMessage(
            """
            §6§l=== Auto Unclaim v${plugin.description.version} ===
            §e/autounclaim run §7- Remove inactive player regions
            §e/autounclaim run --dry §7- Preview what would be removed (dry-run)
            §e/autounclaim reload §7- Reload configuration
            §e/autounclaim §7- Show this help
            §7
            §7Aliases: §f/auc, /unclaim
            §7Inactive period: §f${plugin.inactiveTimeDisplay}
            """.trimIndent()
        )
    }
}