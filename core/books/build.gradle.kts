// File path and name: core/books/build.gradle.kts
// Modified On Timestamp: 2026-10-26 @ 04:56 EDT
// Created On Timestamp: 2026-10-01 @ 15:53 EDT
// File Description: Build of core/books: the bookkeeping services, their tests and the NFR-02 performance test.
// Uses: core/data, core/money, core/i18n, core/importers, core/calc, core/ocr, core/sync; core/data-jdbc and core/replication in tests.
// Used By: app/desktop.
// Purpose: Bookkeeping rules on top of the encrypted storage.
// Bookkeeping rules on top of the encrypted storage: categories, payees, institutions,
// members, accounts and transactions, with validation, permissions and change history.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:data"))
    api(project(":core:money"))
    api(project(":core:i18n"))
    api(project(":core:importers"))
    api(project(":core:calc"))
    api(project(":core:ocr"))
    api(project(":core:sync"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(project(":core:data-jdbc"))
    testImplementation(project(":core:replication"))
    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

// NFR-02: 30 years of history (250,000 transactions). Run with ./gradlew :core:books:performanceTest
tasks.register<Test>("performanceTest") {
    description = "Times screens and reports on 30 years of generated data (NFR-02)"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("performance") }
    maxHeapSize = "2g"
    testLogging { showStandardStreams = true }
    outputs.upToDateWhen { false }
}
