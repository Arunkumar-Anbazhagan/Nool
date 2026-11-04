package com.nool.core.model

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class CalendarEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val calendarId: String = "Personal",
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean = false,
    val colorHex: String = "#6750A4",
    val rrule: String? = null,
    val reminderMinutes: Int = 15,
    val linkedNotePath: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

data class CalendarEventInstance(
    val event: CalendarEvent,
    val instanceStartTime: Long,
    val instanceEndTime: Long,
    val isRecurringOccurrence: Boolean = false
)

data class CalendarCategory(
    val id: String,
    val name: String,
    val colorHex: String,
    val isVisible: Boolean = true
)

enum class RecurrenceFrequency {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

data class ParsedRrule(
    val freq: RecurrenceFrequency,
    val interval: Int = 1,
    val count: Int? = null,
    val until: Long? = null,
    val byDays: List<Int> = emptyList() // Calendar.SUNDAY .. Calendar.SATURDAY
) {
    fun toRruleString(): String? {
        if (freq == RecurrenceFrequency.NONE) return null
        val parts = mutableListOf("FREQ=${freq.name}")
        if (interval > 1) parts.add("INTERVAL=$interval")
        if (count != null && count > 0) parts.add("COUNT=$count")
        if (until != null) parts.add("UNTIL=$until")
        if (byDays.isNotEmpty()) {
            val dayNames = byDays.mapNotNull { day ->
                when (day) {
                    Calendar.SUNDAY -> "SU"
                    Calendar.MONDAY -> "MO"
                    Calendar.TUESDAY -> "TU"
                    Calendar.WEDNESDAY -> "WE"
                    Calendar.THURSDAY -> "TH"
                    Calendar.FRIDAY -> "FR"
                    Calendar.SATURDAY -> "SA"
                    else -> null
                }
            }
            if (dayNames.isNotEmpty()) {
                parts.add("BYDAY=${dayNames.joinToString(",")}")
            }
        }
        return parts.joinToString(";")
    }

    companion object {
        fun parse(rruleString: String?): ParsedRrule {
            if (rruleString.isNullOrBlank()) return ParsedRrule(RecurrenceFrequency.NONE)
            val parts = rruleString.split(";").associate {
                val kv = it.split("=", limit = 2)
                if (kv.size == 2) kv[0].uppercase() to kv[1] else "" to ""
            }

            val freq = when (parts["FREQ"]?.uppercase()) {
                "DAILY" -> RecurrenceFrequency.DAILY
                "WEEKLY" -> RecurrenceFrequency.WEEKLY
                "MONTHLY" -> RecurrenceFrequency.MONTHLY
                "YEARLY" -> RecurrenceFrequency.YEARLY
                else -> RecurrenceFrequency.CUSTOM
            }

            val interval = parts["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val count = parts["COUNT"]?.toIntOrNull()
            val until = parts["UNTIL"]?.toLongOrNull()

            val byDays = parts["BYDAY"]?.split(",")?.mapNotNull { dayStr ->
                when (dayStr.trim().uppercase()) {
                    "SU" -> Calendar.SUNDAY
                    "MO" -> Calendar.MONDAY
                    "TU" -> Calendar.TUESDAY
                    "WE" -> Calendar.WEDNESDAY
                    "TH" -> Calendar.THURSDAY
                    "FR" -> Calendar.FRIDAY
                    "SA" -> Calendar.SATURDAY
                    else -> null
                }
            } ?: emptyList()

            return ParsedRrule(
                freq = freq,
                interval = interval,
                count = count,
                until = until,
                byDays = byDays
            )
        }
    }
}

object RruleExpander {
    private const val MAX_EXPANSIONS = 500

    /**
     * Expands recurring and non-recurring events within the given window [windowStart, windowEnd].
     */
    fun expand(event: CalendarEvent, windowStart: Long, windowEnd: Long): List<CalendarEventInstance> {
        val duration = (event.endTime - event.startTime).coerceAtLeast(0L)
        val rruleStr = event.rrule

        if (rruleStr.isNullOrBlank()) {
            return if (event.endTime >= windowStart && event.startTime <= windowEnd) {
                listOf(
                    CalendarEventInstance(
                        event = event,
                        instanceStartTime = event.startTime,
                        instanceEndTime = event.endTime,
                        isRecurringOccurrence = false
                    )
                )
            } else {
                emptyList()
            }
        }

        val rule = ParsedRrule.parse(rruleStr)
        if (rule.freq == RecurrenceFrequency.NONE) {
            return if (event.endTime >= windowStart && event.startTime <= windowEnd) {
                listOf(
                    CalendarEventInstance(
                        event = event,
                        instanceStartTime = event.startTime,
                        instanceEndTime = event.endTime,
                        isRecurringOccurrence = false
                    )
                )
            } else emptyList()
        }

        val results = mutableListOf<CalendarEventInstance>()
        val cal = Calendar.getInstance()
        cal.timeInMillis = event.startTime

        var occurrencesGenerated = 0
        var loopGuard = 0

        while (loopGuard++ < 10_000 && occurrencesGenerated < (rule.count ?: MAX_EXPANSIONS)) {
            val curStart = cal.timeInMillis
            val curEnd = curStart + duration

            if (rule.until != null && curStart > rule.until) {
                break
            }

            if (curStart > windowEnd) {
                break
            }

            val matchesByDay = if (rule.byDays.isNotEmpty()) {
                cal.get(Calendar.DAY_OF_WEEK) in rule.byDays
            } else {
                true
            }

            if (matchesByDay) {
                occurrencesGenerated++
                if (curEnd >= windowStart && curStart <= windowEnd) {
                    results.add(
                        CalendarEventInstance(
                            event = event,
                            instanceStartTime = curStart,
                            instanceEndTime = curEnd,
                            isRecurringOccurrence = (curStart != event.startTime)
                        )
                    )
                }
            }

            // Advance by frequency
            when (rule.freq) {
                RecurrenceFrequency.DAILY -> {
                    cal.add(Calendar.DAY_OF_YEAR, rule.interval)
                }
                RecurrenceFrequency.WEEKLY -> {
                    if (rule.byDays.isNotEmpty()) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    } else {
                        cal.add(Calendar.WEEK_OF_YEAR, rule.interval)
                    }
                }
                RecurrenceFrequency.MONTHLY -> {
                    cal.add(Calendar.MONTH, rule.interval)
                }
                RecurrenceFrequency.YEARLY -> {
                    cal.add(Calendar.YEAR, rule.interval)
                }
                else -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        return results
    }
}
