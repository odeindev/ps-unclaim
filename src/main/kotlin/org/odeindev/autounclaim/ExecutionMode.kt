package org.odeindev.autounclaim

/**
 * Режим выполнения операции удаления регионов
 */
enum class ExecutionMode {
    /**
     * Реальное удаление регионов из WorldGuard
     */
    REAL,

    /**
     * Dry-run: только анализ без реальных изменений
     */
    DRY_RUN;

    /**
     * Проверяет, нужно ли реально удалять регионы
     */
    fun shouldDelete(): Boolean = this == REAL

    /**
     * Проверяет, является ли режим dry-run
     */
    fun isDryRun(): Boolean = this == DRY_RUN
}