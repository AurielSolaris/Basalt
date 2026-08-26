package app.auriel.basalt.feature.alarm

import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.time.Weekdays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The alarm lifecycle decisions.
 *
 * These are the transitions that are entangled enough to get wrong:
 * snoozing has to re-arm, dismissing has to schedule the *next* occurrence,
 * skipping has to skip one without switching the alarm off, and a one-shot
 * has to end up disabled however it finished.
 */
class AlarmTransitionsTest {

    // 2026-08-26 is a Wednesday.
    private val wednesdayMorning = LocalDateTime.of(2026, 8, 26, 7, 0)

    private fun oneShot(skipNext: Boolean = false) = Alarm(
        id = 1,
        time = LocalTime.of(7, 0),
        repeatDays = Weekdays.None,
        skipNext = skipNext,
    )

    private fun weekdayAlarm(skipNext: Boolean = false) = Alarm(
        id = 2,
        time = LocalTime.of(7, 0),
        repeatDays = Weekdays.Weekdays5,
        skipNext = skipNext,
    )

    // -- after an occurrence ----------------------------------------------

    @Test
    fun `a repeating alarm schedules onward from the occurrence that just ended`() {
        val next = AlarmTransitions.afterOccurrence(weekdayAlarm(), wednesdayMorning)
        assertEquals(AlarmTransitions.Next.ScheduleFrom(wednesdayMorning), next)
    }

    @Test
    fun `it schedules from the occurrence, not from now`() {
        // Dismissing at 07:05 an alarm that rang at 07:00 must walk forward
        // from 07:00. Walking from "now" would work here too, but not when
        // the phone was off and "now" is the following afternoon.
        val next = AlarmTransitions.afterOccurrence(weekdayAlarm(), wednesdayMorning)
        val from = (next as AlarmTransitions.Next.ScheduleFrom).from
        assertEquals(wednesdayMorning, from)

        val armed = AlarmTransitions.resolveFiring(weekdayAlarm(), from)
        assertEquals(LocalDateTime.of(2026, 8, 27, 7, 0), armed.firesAt)
    }

    @Test
    fun `a one-shot switches itself off`() {
        assertEquals(
            AlarmTransitions.Next.DisableAlarm,
            AlarmTransitions.afterOccurrence(oneShot(), wednesdayMorning),
        )
    }

    @Test
    fun `dismissed, missed and skipped agree on what happens next`() {
        // The three callers differ in what they cancel and what they tell
        // the user. They must not differ in the schedule they leave behind.
        val repeating = weekdayAlarm()
        val once = oneShot()
        repeat(3) {
            assertEquals(
                AlarmTransitions.Next.ScheduleFrom(wednesdayMorning),
                AlarmTransitions.afterOccurrence(repeating, wednesdayMorning),
            )
            assertEquals(
                AlarmTransitions.Next.DisableAlarm,
                AlarmTransitions.afterOccurrence(once, wednesdayMorning),
            )
        }
    }

    // -- resolving the firing to arm --------------------------------------

    @Test
    fun `an ordinary alarm arms its next occurrence and touches nothing else`() {
        val firing = AlarmTransitions.resolveFiring(
            weekdayAlarm(),
            LocalDateTime.of(2026, 8, 26, 6, 0),
        )
        assertEquals(LocalDateTime.of(2026, 8, 26, 7, 0), firing.firesAt)
        assertFalse(firing.clearSkipNext)
        assertFalse(firing.disable)
    }

    @Test
    fun `skip next on a repeating alarm takes the occurrence after the skipped one`() {
        val firing = AlarmTransitions.resolveFiring(
            weekdayAlarm(skipNext = true),
            LocalDateTime.of(2026, 8, 26, 6, 0),
        )
        // Wednesday is skipped; Thursday is armed.
        assertEquals(LocalDateTime.of(2026, 8, 27, 7, 0), firing.firesAt)
        assertTrue(firing.clearSkipNext)
        assertFalse(firing.disable)
    }

    @Test
    fun `skip next skips across a weekend for a weekday alarm`() {
        val firing = AlarmTransitions.resolveFiring(
            weekdayAlarm(skipNext = true),
            LocalDateTime.of(2026, 8, 28, 6, 0), // Friday
        )
        assertEquals(DayOfWeek.MONDAY, firing.firesAt?.dayOfWeek)
        assertEquals(LocalDateTime.of(2026, 8, 31, 7, 0), firing.firesAt)
    }

    @Test
    fun `skip next never disables a repeating alarm`() {
        // The whole point of skip-next is that the alarm survives it.
        val firing = AlarmTransitions.resolveFiring(weekdayAlarm(skipNext = true), wednesdayMorning)
        assertFalse(firing.disable)
        assertNotNull(firing.firesAt)
    }

    @Test
    fun `skip next on a one-shot is just off`() {
        val firing = AlarmTransitions.resolveFiring(oneShot(skipNext = true), wednesdayMorning)
        assertNull(firing.firesAt)
        assertTrue(firing.clearSkipNext)
        assertTrue(firing.disable)
    }

    @Test
    fun `the skip flag is consumed exactly once`() {
        // It is cleared when the skip is applied, not when the skipped time
        // passes, so a phone that was off over the skipped morning still
        // comes back in the right state.
        val alarm = weekdayAlarm(skipNext = true)
        val first = AlarmTransitions.resolveFiring(alarm, wednesdayMorning)
        assertTrue(first.clearSkipNext)

        val cleared = alarm.copy(skipNext = false)
        val second = AlarmTransitions.resolveFiring(cleared, wednesdayMorning)
        assertFalse(second.clearSkipNext)
        assertEquals(LocalDateTime.of(2026, 8, 27, 7, 0), second.firesAt)
    }

    @Test
    fun `a repeating alarm run forward for a fortnight never stalls or repeats`() {
        val alarm = weekdayAlarm()
        var occurrence = LocalDateTime.of(2026, 8, 26, 7, 0)
        val seen = mutableSetOf(occurrence)
        repeat(14) {
            val next = AlarmTransitions.afterOccurrence(alarm, occurrence)
            val from = (next as AlarmTransitions.Next.ScheduleFrom).from
            val armed = AlarmTransitions.resolveFiring(alarm, from).firesAt!!
            assertTrue("stalled at $armed", armed.isAfter(occurrence))
            assertTrue("rang twice on $armed", seen.add(armed))
            assertTrue(armed.dayOfWeek in Weekdays.Weekdays5)
            occurrence = armed
        }
    }

    // -- snooze -----------------------------------------------------------

    @Test
    fun `a snooze lands on a whole minute`() {
        val now = LocalDateTime.of(2026, 8, 26, 7, 3, 47, 123_000_000)
        assertEquals(
            LocalDateTime.of(2026, 8, 26, 7, 13, 0, 0),
            AlarmTransitions.snoozeAt(now, 10),
        )
    }

    @Test
    fun `a snooze crosses midnight`() {
        val now = LocalDateTime.of(2026, 8, 26, 23, 55, 30)
        assertEquals(
            LocalDateTime.of(2026, 8, 27, 0, 5, 0),
            AlarmTransitions.snoozeAt(now, 10),
        )
    }

    @Test
    fun `a snooze is always in the future`() {
        val now = LocalDateTime.of(2026, 8, 26, 7, 0, 30)
        for (minutes in 1..60) {
            assertTrue(AlarmTransitions.snoozeAt(now, minutes).isAfter(now))
        }
    }

    // -- request codes ----------------------------------------------------

    @Test
    fun `each slot within an instance is distinct`() {
        val slots = listOf(
            AlarmTransitions.SLOT_FIRE,
            AlarmTransitions.SLOT_UPCOMING,
            AlarmTransitions.SLOT_SHOW,
        )
        val codes = slots.map { AlarmTransitions.requestCode(7L, it) }
        assertEquals(slots.size, codes.toSet().size)
    }

    @Test
    fun `instances cannot collide with each other`() {
        // A collision here means one alarm silently replacing another's
        // pending intent, which reads as an alarm that just never rang.
        val codes = mutableSetOf<Int>()
        for (instanceId in 0L..500L) {
            for (slot in 0 until AlarmTransitions.SLOTS) {
                assertTrue(
                    "collision at instance $instanceId slot $slot",
                    codes.add(AlarmTransitions.requestCode(instanceId, slot)),
                )
            }
        }
    }

    @Test
    fun `slots fit inside the block reserved for an instance`() {
        val slots = listOf(
            AlarmTransitions.SLOT_FIRE,
            AlarmTransitions.SLOT_UPCOMING,
            AlarmTransitions.SLOT_SHOW,
        )
        slots.forEach { assertTrue(it < AlarmTransitions.SLOTS) }
    }
}
