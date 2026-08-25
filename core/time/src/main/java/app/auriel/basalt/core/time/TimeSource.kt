package app.auriel.basalt.core.time

import java.time.Instant
import java.time.ZoneId

/**
 * The app's only source of "now".
 *
 * Nothing below the UI layer is allowed to call [System.currentTimeMillis]
 * or [java.time.LocalTime.now] directly: alarm scheduling, DST rollovers
 * and timer expiry are all untestable without an injectable clock, and
 * those are exactly the parts that have to be right.
 */
interface TimeSource {
    /** Wall-clock time. Jumps when the user or network changes the clock. */
    fun now(): Instant

    /** Monotonic time since boot, for elapsed-time measurement. */
    fun elapsedRealtimeMillis(): Long

    /** The device's current zone. Changes on travel and on DST. */
    fun zone(): ZoneId
}

/** The real device clock. */
class SystemTimeSource : TimeSource {
    override fun now(): Instant = Instant.now()

    override fun elapsedRealtimeMillis(): Long = android.os.SystemClock.elapsedRealtime()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
