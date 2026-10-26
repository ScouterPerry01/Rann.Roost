// File path and name: core/replication/src/main/kotlin/ca/schippers/hfm/replication/HybridClock.kt
// Modified On Timestamp: 2026-10-26 @ 04:07 EDT
// Created On Timestamp: 2026-10-26 @ 04:07 EDT
// File Description: Hybrid logical clock that orders changes across computers (plan-multi-device 4.2).
// Uses: Nothing but the computer's clock.
// Used By: Change capture and the apply engine (phases 2 and 3).
// Purpose: Order changes correctly even when a laptop's clock is wrong, and never give two changes the same time.
package ca.schippers.hfm.replication

/**
 * One moment on the household's shared clock: wall time in milliseconds, a counter for changes in
 * the same millisecond, and the computer that made it. Written as fixed-width text
 * (`0001790000000000-00000-<deviceId>`), so text order is time order and it sorts in SQL too.
 */
data class HlcStamp(val millis: Long, val counter: Int, val device: String) : Comparable<HlcStamp> {
    init {
        require(millis in 0..MAX_MILLIS) { "Clock time out of range: $millis" }
        require(counter in 0..MAX_COUNTER) { "Clock counter out of range: $counter" }
        require(device.isNotEmpty() && device.none { it.isWhitespace() }) { "Clock device is missing" }
    }

    override fun toString(): String = "%016d-%05d-%s".format(millis, counter, device)

    override fun compareTo(other: HlcStamp): Int = compareValuesBy(this, other, { it.millis }, { it.counter }, { it.device })

    companion object {
        const val MAX_COUNTER = 99_999
        const val MAX_MILLIS = 9_999_999_999_999_999L

        private val FORMAT = Regex("^(\\d{16})-(\\d{5})-(\\S+)$")

        /** Reads a stamp written by [toString]; anything else is refused. */
        fun parse(text: String): HlcStamp {
            val m = FORMAT.matchEntire(text) ?: throw IllegalArgumentException("Not a clock value: $text")
            return HlcStamp(m.groupValues[1].toLong(), m.groupValues[2].toInt(), m.groupValues[3])
        }
    }
}

/**
 * The clock of one computer ([device]). [now] stamps a change made here; [receive] moves the clock
 * past a stamp from another computer, so a change made after reading it is ordered after it.
 * Start it from the last stamp this computer saved ([last]) so it never goes back after a restart.
 */
class HybridClock(private val device: String, last: HlcStamp? = null, private val wall: () -> Long = System::currentTimeMillis) {
    private var millis = last?.millis ?: 0L
    private var counter = last?.counter ?: 0

    @Synchronized
    fun now(): HlcStamp {
        val w = wall()
        if (w > millis) {
            millis = w
            counter = 0
        } else {
            tick()
        }
        return HlcStamp(millis, counter, device)
    }

    @Synchronized
    fun receive(remote: HlcStamp): HlcStamp {
        val w = wall()
        val top = maxOf(w, millis, remote.millis)
        when {
            top == millis && top == remote.millis -> { counter = maxOf(counter, remote.counter); tick() }
            top == millis -> tick()
            top == remote.millis -> { millis = top; counter = remote.counter; tick() }
            else -> { millis = top; counter = 0 }
        }
        return HlcStamp(millis, counter, device)
    }

    /** One more change in the same millisecond; past the counter's limit, the next millisecond. */
    private fun tick() {
        if (counter < HlcStamp.MAX_COUNTER) {
            counter++
        } else {
            millis++
            counter = 0
        }
    }

    companion object {
        /** A change stamped further ahead than this is held, with a warning (plan 4.2). */
        const val MAX_AHEAD_MILLIS = 24L * 60 * 60 * 1000

        /** True when [stamp] is more than a day ahead of this computer's clock: the other computer's clock is wrong. */
        fun tooFarAhead(stamp: HlcStamp, wallNow: Long = System.currentTimeMillis()): Boolean = stamp.millis - wallNow > MAX_AHEAD_MILLIS
    }
}
