# AutoUnclaim

Плагин для Minecraft, который автоматически удаляет регионы ProtectionStones у неактивных игроков.

## Возможности

-  **Гибкая настройка времени** - минуты, часы или дни
-  **Автоматическая проверка** - по расписанию
- ️ **Защита онлайн игроков** - не трогает активных игроков
-  **Подробные логи** - всё видно в консоли

## Требования

- **Сервер**: Purpur/Paper 1.18.2+
- **Java**: 17 или выше
- **Зависимости**:
  - WorldGuard 7.0+
  - ProtectionStones

## Установка

1. Скачайте последнюю версию из [Releases](../../releases)
2. Поместите `autounclaim-1.0.0.jar` в папку `plugins/`
3. Убедитесь что WorldGuard и ProtectionStones установлены
4. Запустите сервер
5. Настройте `plugins/AutoUnclaim/config.yml`

## Настройка

```yaml
# Период неактивности
inactive-time:
  value: 90          # Число
  unit: days         # Единица: minutes / hours / days

# Автоматический запуск
auto-run:
  enabled: true      # Включить автопроверку
  interval-minutes: 60  # Проверять каждые 60 минут
```

### Примеры конфигураций

**Для тестирования (проверка каждые 5 минут):**
```yaml
inactive-time:
  value: 1
  unit: minutes

auto-run:
  enabled: true
  interval-minutes: 5
```

**Для маленького сервера:**
```yaml
inactive-time:
  value: 30
  unit: days

auto-run:
  enabled: true
  interval-minutes: 60  # Каждый час
```

**Для большого сервера:**
```yaml
inactive-time:
  value: 90
  unit: days

auto-run:
  enabled: true
  interval-minutes: 1440  # Раз в сутки (24 часа)
```

## Команды

| Команда | Описание |
|---------|----------|
| `/autounclaim` | Показать справку |
| `/autounclaim run` | Запустить проверку вручную |
| `/autounclaim reload` | Перезагрузить конфиг |

**Алиасы:** `/auc`, `/unclaim`

## Права доступа

```yaml
autounclaim.use:
  default: op  # Доступ к командам
```

## Как это работает

1. Плагин запускается автоматически по расписанию
2. Проверяет всех оффлайн игроков
3. Находит тех, кто не заходил дольше указанного времени
4. Удаляет их регионы ProtectionStones (с префиксом `ps`)
5. Логирует результаты в консоль

**Важно:** Онлайн игроки всегда защищены от удаления!

## Сборка из исходников

### Требования
- JDK 17+
- Gradle 7.0+

### Команды

```bash
# Клонировать репозиторий
git clone https://github.com/yourusername/ps-unclaim.git
cd ps-unclaim

# Собрать плагин
./gradlew shadowJar

# Готовый файл
build/libs/autounclaim-1.0.0.jar
```

## Структура проекта

```
ps-unclaim/
├── src/main/
│   ├── kotlin/org/odeindev/autounclaim/
│   │   ├── AutoUnclaim.kt       # Главный класс
│   │   ├── RegionPruner.kt      # Логика удаления
│   │   └── UnclaimCommand.kt    # Команды
│   └── resources/
│       ├── plugin.yml
│       └── config.yml
└── build.gradle.kts
```

## Примеры логов

**При запуске сервера:**
```
[AutoUnclaim] Plugin loaded! Inactive period: 90 дней
[AutoUnclaim] Auto-run enabled! Interval: 60 minute(s)
[AutoUnclaim] Auto-check started (every 60 minute(s))
```

**При автопроверке:**
```
[AutoUnclaim] Auto-run: Starting inactive player check...
[AutoUnclaim] Found 2 inactive player(s) (not logged in > 90 дней)
[AutoUnclaim] Removed region: ps123456 (world: world)
[AutoUnclaim] Auto-run completed: removed 2 region(s), affected 2 player(s)
```

## Частые вопросы

**В: Удалит ли регионы у игроков онлайн?**  
О: Нет. Плагин автоматически исключает онлайн игроков.

**В: Можно протестировать без ожидания 90 дней?**  
О: Да! Установите `inactive-time: value: 1, unit: minutes` в конфиге.

**В: Как отключить автопроверку?**  
О: Установите `auto-run.enabled: false` в конфиге.

**В: Работает ли с другими плагинами регионов?**  
О: Только с ProtectionStones (регионы с префиксом `ps`).

## Решение проблем

**Плагин не загружается:**
- Проверьте что WorldGuard и ProtectionStones установлены
- Убедитесь что Java 17 или выше
- Посмотрите логи на ошибки

**Регионы не удаляются:**
- Проверьте что ID региона начинается с `ps`
- Убедитесь что игрок действительно оффлайн
- Проверьте что прошёл период неактивности

**OutOfMemoryError:**
- Увеличьте RAM сервера: `-Xmx4G` вместо `-Xmx2G`

## Технологии

- **Язык**: Kotlin 1.8.0
- **Сборка**: Gradle (Kotlin DSL)
- **API**: Paper API, WorldGuard API
- **Плагин**: ShadowJar

## Changelog

### v1.0.0 (2026-02-15)
- Первый релиз
- Автоматические проверки по расписанию
- Защита онлайн игроков
- Гибкие единицы времени (минуты/часы/дни)
- Ручной запуск команд
- Подробное логирование

## Лицензия

MIT License - см. [LICENSE](LICENSE)

## Автор

**odeindev** - [GitHub](https://github.com/odeindev)

## Поддержка

Если вы нашли баг или есть вопросы:
- Откройте [Issue](../../issues)
- Проверьте существующие issues
- Посмотрите раздел [Частые вопросы](#частые-вопросы)

---

**Сделано для сообщества Minecraft** ❤️