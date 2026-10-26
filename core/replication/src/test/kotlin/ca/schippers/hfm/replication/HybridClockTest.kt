// File path and name: core/replication/src/test/kotlin/ca/schippers/hfm/replication/HybridClockTest.kt
// Modified On Timestamp: 2026-10-26 @ 04:07 EDT
// Created On Timestamp: 2026-10-26 @ 04:07 EDT
// File Description: Tests of the hybrid logical clock (plan-multi-device 4.2).
// Uses: HybridClock, HlcStamp.
// Used By: ./gradlew :core:replication:test.
// Purpose: Changes stay in order with wrong clocks, restarts and other computers' stamps.
package ca.schippers.hfm.replication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HybridClockTest {

    private val a = "0192f0a1-0000-7000-8000-00000000000a"
    private val b = "0192f0a1-0000-7000-8000-00000000000b"

    @Test
    fun `stamps keep moving forward when the computer clock goes back, and sort as text`() {
        var wall = 1_790_000_000_000L
        val clock = HybridClock(a) { wall }
        val first = clock.now()
        wall -= 60_000 // the clock is set back a minute
        val second = clock.now()
        val third = clock.now()
        wall += 120_000
        val fourth = clock.now()
        val stamps = listOf(first, second, third, fourth)
        assertEquals(stamps, stamps.sorted())
        assertEquals(stamps.map { it.toString() }, stamps.map { it.toString() }.sorted())
        assertEquals(stamps.size, stamps.toSet().size)
        assertEquals(1, second.counter)
        assertEquals(0, fourth.counter)
    }

    @Test
    fun `a stamp from a computer whose clock is ahead moves this clock past it`() {
        val wall = 1_790_000_000_000L
        val clock = HybridClock(a) { wall }
        val remote = HlcStamp(wall + 5_000, 7, b)
        val received = clock.receive(remote)
        assertTrue(received > remote)
        assertEquals(wall + 5_000, received.millis)
        assertEquals(8, received.counter)
        assertTrue(clock.now() > received, "a change made after reading it comes after it")
    }

    @Test
    fun `an older remote stamp does not move the clock back`() {
        val wall = 1_790_000_000_000L
        val clock = HybridClock(a) { wall }
        val mine = clock.now()
        assertTrue(clock.receive(HlcStamp(wall - 10_000, 3, b)) > mine)
    }

    @Test
    fun `after a restart the clock continues from the last saved stamp`() {
        val saved = HlcStamp(1_790_000_000_500L, 4, a)
        val clock = HybridClock(a, last = saved) { 1_790_000_000_000L } // the clock went back while closed
        val next = clock.now()
        assertTrue(next > saved)
        assertEquals(HlcStamp(1_790_000_000_500L, 5, a), next)
    }

    @Test
    fun `a full counter moves to the next millisecond`() {
        val clock = HybridClock(a, last = HlcStamp(1_000L, HlcStamp.MAX_COUNTER, a)) { 1_000L }
        assertEquals(HlcStamp(1_001L, 0, a), clock.now())
    }

    @Test
    fun `stamps are written and read back exactly, and other text is refused`() {
        val stamp = HlcStamp(1_790_000_000_123L, 42, a)
        assertEquals("0001790000000123-00042-$a", stamp.toString())
        assertEquals(stamp, HlcStamp.parse(stamp.toString()))
        for (bad in listOf("", "1790000000123-00042-$a", "0001790000000123-42-$a", "0001790000000123-00042-", "x001790000000123-00042-$a")) {
            assertFailsWith<IllegalArgumentException>(bad) { HlcStamp.parse(bad) }
        }
    }

    @Test
    fun `a change dated more than a day ahead is held`() {
        val now = 1_790_000_000_000L
        assertFalse(HybridClock.tooFarAhead(HlcStamp(now + 60_000, 0, b), now))
        assertTrue(HybridClock.tooFarAhead(HlcStamp(now + HybridClock.MAX_AHEAD_MILLIS + 1, 0, b), now))
    }

    @Test
    fun `stamps of two computers at the same moment are ordered by computer, never equal`() {
        val x = HybridClock(a) { 5_000L }.now()
        val y = HybridClock(b) { 5_000L }.now()
        assertTrue(x < y)
    }
}
