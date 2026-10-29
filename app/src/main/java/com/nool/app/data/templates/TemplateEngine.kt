package com.nool.app.data.templates

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TemplateEngine {

    private val DATE_FORMAT_REGEX = Regex("""\{\{date:([^}]+)\}\}""", RegexOption.IGNORE_CASE)
    private val TIME_FORMAT_REGEX = Regex("""\{\{time:([^}]+)\}\}""", RegexOption.IGNORE_CASE)

    fun processTemplate(
        templateText: String,
        noteTitle: String,
        currentDate: Date = Date(),
        locale: Locale = Locale.getDefault()
    ): String {
        val defaultDateFormat = SimpleDateFormat("yyyy-MM-dd", locale)
        val defaultTimeFormat = SimpleDateFormat("HH:mm", locale)

        var result = templateText
            .replace("{{title}}", noteTitle, ignoreCase = true)
            .replace("{{date}}", defaultDateFormat.format(currentDate), ignoreCase = true)
            .replace("{{time}}", defaultTimeFormat.format(currentDate), ignoreCase = true)

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
}
