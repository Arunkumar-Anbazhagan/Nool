package com.nool.core.model

import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CalendarRecurrenceTest {

    @Test
    fun testNonRecurringEventInsideAndOutsideWindow() {
        val event = CalendarEvent(
            id = "event-1",
            title = "One-time Meeting",
            startTime = 1_000_000L,
            endTime = 1_003_600L
        )

        // Inside window
        val inside = RruleExpander.expand(event, windowStart = 900_000L, windowEnd = 2_000_000L)
        assertEquals(1, inside.size)
        assertEquals(1_000_000L, inside[0].instanceStartTime)
        assertEquals(false, inside[0].isRecurringOccurrence)

        // Outside window (before)
        val before = RruleExpander.expand(event, windowStart = 2_000_000L, windowEnd = 3_000_000L)
        assertTrue(before.isEmpty())

        // Outside window (after)
        val after = RruleExpander.expand(event, windowStart = 100_000L, windowEnd = 500_000L)
        assertTrue(after.isEmpty())
    }

    @Test
    fun testDailyRecurrence() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = baseCal.timeInMillis
        val endTime = startTime + 3600_000L

        val event = CalendarEvent(
            id = "daily-1",
            title = "Daily Standup",
            startTime = startTime,
            endTime = endTime,
            rrule = "FREQ=DAILY;INTERVAL=1"
        )

        // Query for 5 days: Nov 1 to Nov 5
        val windowEndCal = (baseCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 4)
            set(Calendar.HOUR_OF_DAY, 23)
        }

        val instances = RruleExpander.expand(event, windowStart = startTime, windowEnd = windowEndCal.timeInMillis)
        assertEquals(5, instances.size)
        assertEquals(false, instances[0].isRecurringOccurrence)
        assertEquals(true, instances[1].isRecurringOccurrence)
    }

    @Test
    fun testWeeklyRecurrenceWithSpecificDays() {
        // Monday Nov 2, 2026
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2, 9, 0, 0) // Nov 2, 2026 is a Monday
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = baseCal.timeInMillis
        val endTime = startTime + 1800_000L // 30 mins

        // MWF recurrence
        val event = CalendarEvent(
            id = "mwf-1",
            title = "Gym Workout",
            startTime = startTime,
            endTime = endTime,
            rrule = "FREQ=WEEKLY;BYDAY=MO,WE,FR"
        )

        // Query 2 weeks
        val windowEndCal = (baseCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 13)
        }

        val instances = RruleExpander.expand(event, windowStart = startTime, windowEnd = windowEndCal.timeInMillis)
        // 2 weeks x 3 days (Mon, Wed, Fri) = 6 occurrences
        assertEquals(6, instances.size)
    }

    @Test
    fun testRecurrenceCountLimit() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = baseCal.timeInMillis
        val endTime = startTime + 3600_000L

        // Daily repeating with COUNT=3
        val event = CalendarEvent(
            id = "count-1",
            title = "Sprint Planning Retries",
            startTime = startTime,
            endTime = endTime,
            rrule = "FREQ=DAILY;COUNT=3"
        )

        // 30 day window, but COUNT is 3
        val windowEndCal = (baseCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 30)
        }

        val instances = RruleExpander.expand(event, windowStart = startTime, windowEnd = windowEndCal.timeInMillis)
        assertEquals(3, instances.size)
    }

    @Test
    fun testRecurrenceUntilLimit() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = baseCal.timeInMillis
        val endTime = startTime + 3600_000L

        val untilCal = (baseCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 3)
            set(Calendar.HOUR_OF_DAY, 12)
        }

        val event = CalendarEvent(
            id = "until-1",
            title = "Conference Sessions",
            startTime = startTime,
            endTime = endTime,
            rrule = "FREQ=DAILY;UNTIL=${untilCal.timeInMillis}"
        )

        val windowEndCal = (baseCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 10)
        }

        val instances = RruleExpander.expand(event, windowStart = startTime, windowEnd = windowEndCal.timeInMillis)
        // Should include Nov 1, Nov 2, Nov 3, Nov 4 (since until is Nov 4 12:00 and session is 8:00), but not Nov 5+
        assertEquals(4, instances.size)
    }

    @Test
    fun testParsedRruleRoundTrip() {
        val original = "FREQ=WEEKLY;INTERVAL=2;COUNT=10;BYDAY=TU,TH"
        val parsed = ParsedRrule.parse(original)
        assertEquals(RecurrenceFrequency.WEEKLY, parsed.freq)
        assertEquals(2, parsed.interval)
        assertEquals(10, parsed.count)
        assertEquals(2, parsed.byDays.size)
        assertTrue(parsed.byDays.contains(Calendar.TUESDAY))
        assertTrue(parsed.byDays.contains(Calendar.THURSDAY))

        val serialized = parsed.toRruleString()
        assertNotNull(serialized)
        assertTrue(serialized.contains("FREQ=WEEKLY"))
        assertTrue(serialized.contains("INTERVAL=2"))
        assertTrue(serialized.contains("COUNT=10"))
        assertTrue(serialized.contains("BYDAY=TU,TH"))
    }
}
