import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.odeindev"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper API
    maven("https://maven.enginehub.org/repo/") // WorldGuard
}

dependencies {
    // Kotlin stdlib
    implementation(kotlin("stdlib"))

    // Paper API (Purpur совместим с Paper)
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")

    // WorldGuard API
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.7")

    // ProtectionStones API (если есть maven репозиторий, замени на правильный)
    // compileOnly("...") // Добавь если нужно
}

tasks {
    // Настройка компиляции Kotlin
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    // Настройка ShadowJar для вшивания Kotlin stdlib
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")

        // Relocate Kotlin чтобы избежать конфликтов с другими плагинами
        relocate("kotlin", "org.odeindev.autounclaim.libs.kotlin")

        // Минимизация jar файла
        minimize()
    }

    // Делаем shadowJar главной задачей сборки
    build {
        dependsOn(shadowJar)
    }
}

// Java версия
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}