// File path and name: core/replication/build.gradle.kts
// Modified On Timestamp: 2026-10-26 @ 04:36 EDT
// Created On Timestamp: 2026-10-26 @ 03:45 EDT
// File Description: Build of core/replication: sharing one household between computers (ADR 0011).
// Uses: core/data (schemas, SchemaManager); core/data-jdbc in tests.
// Used By: settings.gradle.kts; app/desktop tests (CaptureCompletenessTest); later core/books and app/desktop.
// Purpose: Keep the multi-computer code in a module the phone app never builds (plan-multi-device section 7).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:data"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(project(":core:data-jdbc"))
    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}
