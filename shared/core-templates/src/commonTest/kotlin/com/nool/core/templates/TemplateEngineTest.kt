package com.nool.core.templates

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateEngineTest {

    @Test
    fun testBasicPlaceholders() {
        // Fixed timestamp: 2026-11-03T10:30:00Z
        val instant = Instant.parse("2026-11-03T10:30:00Z")
        val tz = TimeZone.UTC

        val template = "# {{title}}\nCreated on {{date}} at {{time}}"
        val result = TemplateEngine.processTemplate(template, "My Meeting", instant, tz)

        val expected = "# My Meeting\nCreated on 2026-11-03 at 10:30"
        assertEquals(expected, result)
    }

    @Test
    fun testCustomPattern() {
        val instant = Instant.parse("2026-11-03T10:30:00Z")
        val tz = TimeZone.UTC

        val template = "Year: {{date:yyyy}}, Day: {{date:dd}}"
        val result = TemplateEngine.processTemplate(template, "Note", instant, tz)

        assertTrue(result.contains("Year: 2026"))
        assertTrue(result.contains("Day: 03"))
    }
}
