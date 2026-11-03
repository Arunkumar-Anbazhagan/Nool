package com.nool.core.templates

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TemplateEngine {

    private val DATE_FORMAT_REGEX = Regex("""\{\{date:([^}]+)\}\}""", RegexOption.IGNORE_CASE)
    private val TIME_FORMAT_REGEX = Regex("""\{\{time:([^}]+)\}\}""", RegexOption.IGNORE_CASE)

    fun processTemplate(
        templateText: String,
        noteTitle: String,
        nowInstant: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val ldt = nowInstant.toLocalDateTime(timeZone)
        val defaultDateStr = "${ldt.year}-${ldt.monthNumber.toString().padStart(2, '0')}-${ldt.dayOfMonth.toString().padStart(2, '0')}"
        val defaultTimeStr = "${ldt.hour.toString().padStart(2, '0')}:${ldt.minute.toString().padStart(2, '0')}"

        val currentDate = Date(nowInstant.toEpochMilliseconds())
        val defaultDateFormat = SimpleDateFormat("yyyy-MM-dd", locale)
        val defaultTimeFormat = SimpleDateFormat("HH:mm", locale)

        var result = templateText
            .replace("{{title}}", noteTitle, ignoreCase = true)
            .replace("{{date}}", defaultDateStr, ignoreCase = true)
            .replace("{{time}}", defaultTimeStr, ignoreCase = true)

        // Custom {{date:PATTERN}} formats
        result = DATE_FORMAT_REGEX.replace(result) { match ->
            val pattern = match.groupValues[1].trim()
            try {
                SimpleDateFormat(pattern, locale).format(currentDate)
            } catch (e: Exception) {
                defaultDateFormat.format(currentDate)
            }
        }

        // Custom {{time:PATTERN}} formats
        result = TIME_FORMAT_REGEX.replace(result) { match ->
            val pattern = match.groupValues[1].trim()
            try {
                SimpleDateFormat(pattern, locale).format(currentDate)
            } catch (e: Exception) {
                defaultTimeFormat.format(currentDate)
            }
        }

        return result
    }

    // Overload for backward compatibility with existing java.util.Date calls
    fun processTemplate(
        templateText: String,
        noteTitle: String,
        currentDate: Date,
        locale: Locale = Locale.getDefault()
    ): String {
        return processTemplate(
            templateText = templateText,
            noteTitle = noteTitle,
            nowInstant = Instant.fromEpochMilliseconds(currentDate.time),
            timeZone = TimeZone.currentSystemDefault(),
            locale = locale
        )
    }
}
