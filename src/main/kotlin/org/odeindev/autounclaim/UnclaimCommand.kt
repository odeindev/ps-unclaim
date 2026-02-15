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
            "run", "start" -> handleRun(sender)
            "reload" -> handleReload(sender)
            else -> handleHelp(sender)
        }

        return true
    }

    /**
     * Запуск проверки и удаление регионов
     */
    private fun handleRun(sender: CommandSender) {
        sender.sendMessage("§e[AutoUnclaim] Starting inactive player check...")

        // Запускаем асинхронно, чтобы не блокировать главный поток
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val pruner = RegionPruner(plugin)
            val result = pruner.pruneInactiveRegions()

            // Возвращаемся в главный поток для отправки сообщения
            plugin.server.scheduler.runTask(plugin, Runnable {
                sender.sendMessage(
                    """
                    §a[AutoUnclaim] Check completed!
                    §7Removed regions: §f${result.removedRegions}
                    §7Affected players: §f${result.affectedPlayers.size}
                    """.trimIndent()
                )

                if (result.affectedPlayers.isNotEmpty()) {
                    plugin.logger.info("[AutoUnclaim] Affected players: ${result.affectedPlayers.joinToString()}")
                }
            })
        })
    }

    /**
     * Перезагрузка конфигурации
     */
    private fun handleReload(sender: CommandSender) {
        plugin.reloadConfiguration()
        sender.sendMessage("§a[AutoUnclaim] Config reloaded! Period: ${plugin.inactiveTimeDisplay}")
    }

    /**
     * Показать справку по командам
     */
    private fun handleHelp(sender: CommandSender) {
        sender.sendMessage(
            """
            §6=== Auto Unclaim ===
            §e/autounclaim run §7- Start check
            §e/autounclaim reload §7- Reload config
            §e/autounclaim §7- Show this help
            """.trimIndent()
        )
    }
}