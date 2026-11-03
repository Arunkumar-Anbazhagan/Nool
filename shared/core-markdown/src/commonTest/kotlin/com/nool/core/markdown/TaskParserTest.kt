package com.nool.core.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskParserTest {

    @Test
    fun testParseTasksWithDueDates() {
        val markdown = """
            # Project Notes
            
            - [ ] Buy groceries 📅 2026-11-04
            - [x] Finished report 📅 2026-11-01
            * [ ] Call dentist 📅 2026-11-01
            + [ ] Learn Kotlin Multiplatform
            Regular text line.
        """.trimIndent()

        val tasks = TaskParser.parseTasks("proj.md", "Project Notes", markdown, todayStr = "2026-11-04")
        assertEquals(4, tasks.size)

        // Task 1: Buy groceries (due today)
        assertEquals("Buy groceries", tasks[0].text)
        assertFalse(tasks[0].isCompleted)
        assertEquals("2026-11-04", tasks[0].dueDate)
        assertTrue(tasks[0].isDueToday)
        assertFalse(tasks[0].isOverdue)

        // Task 2: Finished report (completed, so not overdue even if date was past)
        assertEquals("Finished report", tasks[1].text)
        assertTrue(tasks[1].isCompleted)
        assertEquals("2026-11-01", tasks[1].dueDate)
        assertFalse(tasks[1].isOverdue)
        assertFalse(tasks[1].isDueToday)

        // Task 3: Call dentist (overdue)
        assertEquals("Call dentist", tasks[2].text)
        assertFalse(tasks[2].isCompleted)
        assertEquals("2026-11-01", tasks[2].dueDate)
        assertTrue(tasks[2].isOverdue)
        assertFalse(tasks[2].isDueToday)

        // Task 4: Learn Kotlin Multiplatform (no due date)
        assertEquals("Learn Kotlin Multiplatform", tasks[3].text)
        assertFalse(tasks[3].isCompleted)
        assertEquals(null, tasks[3].dueDate)
        assertFalse(tasks[3].isOverdue)
        assertFalse(tasks[3].isDueToday)
    }
}
