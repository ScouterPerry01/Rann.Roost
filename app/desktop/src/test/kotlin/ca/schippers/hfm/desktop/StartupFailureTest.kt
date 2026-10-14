// File path and name: app/desktop/src/test/kotlin/ca/schippers/hfm/desktop/StartupFailureTest.kt
// Modified On Timestamp: 2026-10-13 @ 18:05 EDT
// Created On Timestamp: 2026-10-13 @ 18:05 EDT
// File Description: Tests of what the start-up error message shows.
// Uses: StartupFailure.
// Used By: ./gradlew :app:desktop:test.
// Purpose: A screenshot of the start-up message must name the real cause, even when it is wrapped.

package ca.schippers.hfm.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StartupFailureTest {

    @Test
    fun `the message names the root cause and where it happened`() {
        val root = UnsatisfiedLinkError("no sqlcipher in java.library.path")
        val text = StartupFailure.summary(IllegalStateException("Cannot open the household store", RuntimeException("wrapped", root)))
        val lines = text.lines()
        assertEquals("java.lang.IllegalStateException: Cannot open the household store", lines[0])
        assertEquals("Caused by: java.lang.UnsatisfiedLinkError: no sqlcipher in java.library.path", lines[1])
        assertTrue(lines[2].startsWith("    at ca.schippers.hfm.desktop.StartupFailureTest"), lines[2])
        assertTrue(lines.size <= 2 + 8)
    }

    @Test
    fun `an error without a cause is not repeated`() {
        val text = StartupFailure.summary(IllegalArgumentException("bad"), frames = 2)
        assertFalse("Caused by" in text)
        assertEquals(3, text.lines().size)
    }
}
