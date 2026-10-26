// File path and name: settings.gradle.kts
// Modified On Timestamp: 2026-10-26 @ 03:46 EDT
// Created On Timestamp: 2026-10-01 @ 15:53 EDT
// File Description: Gradle settings: repositories and the list of modules in the build.
// Uses: gradle/libs.versions.toml (through the module build files).
// Used By: Gradle, CI (.github/workflows/ci.yml).
// Purpose: One place that says which modules make up RANN's Roost.
rootProject.name = "household-finance-manager"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(
    ":core:money",
    ":core:calc",
    ":core:domain",
    ":core:security",
    ":core:data",
    ":core:data-jdbc",
    ":core:books",
    ":core:i18n",
    ":core:importers",
    ":core:sync",
    ":core:ocr",
    ":core:ocr-desktop",
    ":core:ai",
    ":core:update",
    ":core:replication",
    ":app:desktop",
    ":tools:release",
)
