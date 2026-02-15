package org.odeindev.autounclaim

import org.bukkit.plugin.java.JavaPlugin

/**
 * Главный класс плагина AutoUnclaim
 * Управляет жизненным циклом плагина и конфигурацией
 */
class AutoUnclaim : JavaPlugin() {

    companion object {
        lateinit var instance: AutoUnclaim
            private set
    }

    // Значения конфигурации
    var inactiveMillis: Long = 0L
        private set

    var inactiveTimeDisplay: String = "90 дней"
        private set

    var autoRunEnabled: Boolean = false
        private set

    var autoRunIntervalMinutes: Int = 60
        private set

    private var autoRunTaskId: Int? = null

    /**
     * Вызывается при включении плагина
     */
    override fun onEnable() {
        instance = this

        loadConfiguration()
        getCommand("autounclaim")?.setExecutor(UnclaimCommand(this))

        if (autoRunEnabled) {
            startAutoRun()
        }

        logger.info("[AutoUnclaim] Plugin loaded! Inactive period: $inactiveTimeDisplay")
    }

    /**
     * Вызывается при выключении плагина
     */
    override fun onDisable() {
        stopAutoRun()
        logger.info("[AutoUnclaim] Plugin unloaded")
    }

    /**
     * Загрузка конфигурации из config.yml
     */
    private fun loadConfiguration() {
        saveDefaultConfig()

        val timeValue = config.getInt("inactive-time.value", 90)
        val timeUnit = config.getString("inactive-time.unit", "days")?.lowercase() ?: "days"

        if (timeValue < 1) {
            logger.warning("[AutoUnclaim] inactive-time.value must be >= 1. Using default: 90 days.")
            inactiveMillis = java.time.Duration.ofDays(90).toMillis()
            inactiveTimeDisplay = "90 дней"
            return
        }

        // Конвертируем в миллисекунды в зависимости от единицы времени
        inactiveMillis = when (timeUnit) {
            "minutes", "minute", "min", "м", "минут", "минуты" -> {
                inactiveTimeDisplay = "$timeValue минут(ы)"
                java.time.Duration.ofMinutes(timeValue.toLong()).toMillis()
            }
            "hours", "hour", "h", "ч", "часов", "часа" -> {
                inactiveTimeDisplay = "$timeValue час(ов)"
                java.time.Duration.ofHours(timeValue.toLong()).toMillis()
            }
            "days", "day", "d", "д", "дней", "дня" -> {
                inactiveTimeDisplay = "$timeValue дней"
                java.time.Duration.ofDays(timeValue.toLong()).toMillis()
            }
            else -> {
                logger.warning("[AutoUnclaim] Unknown time unit '$timeUnit'. Using days.")
                inactiveTimeDisplay = "$timeValue дней"
                java.time.Duration.ofDays(timeValue.toLong()).toMillis()
            }
        }

        logger.info("[AutoUnclaim] Inactive period: $inactiveTimeDisplay")

        // Загружаем настройки автозапуска
        autoRunEnabled = config.getBoolean("auto-run.enabled", false)
        autoRunIntervalMinutes = config.getInt("auto-run.interval-minutes", 60).let {
            if (it < 1) {
                logger.warning("[AutoUnclaim] auto-run.interval-minutes must be >= 1. Using default: 60 minutes.")
                60
            } else {
                it
            }
        }

        if (autoRunEnabled) {
            logger.info("[AutoUnclaim] Auto-run enabled! Interval: $autoRunIntervalMinutes minute(s)")
        }
    }

    /**
     * Перезагрузка конфигурации и перезапуск автопроверки при необходимости
     */
    fun reloadConfiguration() {
        stopAutoRun()
        reloadConfig()
        loadConfiguration()

        if (autoRunEnabled) {
            startAutoRun()
        }
    }

    /**
     * Запуск автоматических проверок по расписанию
     */
    private fun startAutoRun() {
        // Конвертируем минуты в тики (1 минута = 1200 тиков, 1 тик = 50ms)
        val intervalTicks = autoRunIntervalMinutes * 20L * 60L

        autoRunTaskId = server.scheduler.runTaskTimerAsynchronously(
            this,
            Runnable {
                logger.info("[AutoUnclaim] Auto-run: Starting inactive player check...")
                val pruner = RegionPruner(this)
                val result = pruner.pruneInactiveRegions()

                if (result.removedRegions > 0) {
                    logger.info(
                        "[AutoUnclaim] Auto-run completed: removed ${result.removedRegions} region(s), " +
                                "affected ${result.affectedPlayers.size} player(s)"
                    )
                }
            },
            intervalTicks,
            intervalTicks
        ).taskId

        logger.info("[AutoUnclaim] Auto-check started (every $autoRunIntervalMinutes minute(s))")
    }

    /**
     * Остановка автоматических проверок
     */
    private fun stopAutoRun() {
        autoRunTaskId?.let { taskId ->
            server.scheduler.cancelTask(taskId)
            autoRunTaskId = null
            logger.info("[AutoUnclaim] Auto-check stopped")
        }
    }
}