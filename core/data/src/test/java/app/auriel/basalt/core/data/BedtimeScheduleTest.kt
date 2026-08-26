package app.auriel.basalt.core.data

import app.auriel.basalt.core.data.model.BedtimeSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * The sleep window.
 *
 * Almost every sleep window wraps past midnight, so the wrap is the normal
 * case rather than the edge case, and getting it wrong shows up as a
 * negative duration or an arc drawn the wrong way round the dial.
 */
class BedtimeScheduleTest {

    @Test
    fun `the default window wraps midnight`() {
        val schedule = BedtimeSchedule()
        assertEquals(LocalTime.of(23, 0), schedule.bedtime)
        assertEquals(LocalTime.of(7, 0), schedule.wakeTime)
        assertEquals(8 * 60, schedule.durationMinutes)
    }

    @Test
    fun `a window inside one day does not wrap`() {
        val nap = BedtimeSchedule(
            bedtime = LocalTime.of(13, 0),
            wakeTime = LocalTime.of(14, 30),
        )
        assertEquals(90, nap.durationMinutes)
    }

    @Test
    fun `a window starting at midnight is measured forwards`() {
        val schedule = BedtimeSchedule(
            bedtime = LocalTime.MIDNIGHT,
            wakeTime = LocalTime.of(6, 30),
        )
        assertEquals(390, schedule.durationMinutes)
    }

    @Test
    fun `a window ending at midnight is a full day short of nothing`() {
        // Wake at exactly 00:00 after a 23:00 bedtime: the end is not after
        // the start, so it wraps, and the answer is one hour.
        val schedule = BedtimeSchedule(
            bedtime = LocalTime.of(23, 0),
            wakeTime = LocalTime.MIDNIGHT,
        )
        assertEquals(60, schedule.durationMinutes)
    }

    @Test
    fun `the duration is never negative for any pair of times`() {
        for (bedMinute in 0 until 24 * 60 step 17) {
            for (wakeMinute in 0 until 24 * 60 step 23) {
                val schedule = BedtimeSchedule(
                    bedtime = LocalTime.ofSecondOfDay(bedMinute * 60L),
                    wakeTime = LocalTime.ofSecondOfDay(wakeMinute * 60L),
                )
                val duration = schedule.durationMinutes
                assert(duration in 0..(24 * 60)) {
                    "bed $bedMinute wake $wakeMinute produced $duration"
                }
            }
        }
    }

    @Test
    fun `wind down is a fixed lead before bedtime`() {
        val schedule = BedtimeSchedule(bedtime = LocalTime.of(23, 0), windDownMinutes = 30)
        assertEquals(LocalTime.of(22, 30), schedule.windDownAt)
    }

    @Test
    fun `wind down wraps back past midnight`() {
        val schedule = BedtimeSchedule(bedtime = LocalTime.of(0, 15), windDownMinutes = 30)
        assertEquals(LocalTime.of(23, 45), schedule.windDownAt)
    }

    @Test
    fun `wind down of zero is bedtime itself`() {
        val schedule = BedtimeSchedule(bedtime = LocalTime.of(22, 0), windDownMinutes = 0)
        assertEquals(LocalTime.of(22, 0), schedule.windDownAt)
    }

    @Test
    fun `the default day mask is every day`() {
        assertEquals(7, Integer.bitCount(BedtimeSchedule().days))
    }
}
