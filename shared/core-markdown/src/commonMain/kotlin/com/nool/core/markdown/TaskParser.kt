package com.nool.core.markdown

data class TaskItem(
    val notePath: String,
    val noteTitle: String,
    val lineIndex: Int,
    val text: String,
    val isCompleted: Boolean,
    val dueDate: String? = null,
    val isOverdue: Boolean = false,
    val isDueToday: Boolean = false
)

object TaskParser {

    private val TASK_REGEX = Regex("""^\s*[-*+]\s+\[([ xX])\]\s+(.+)$""")
    private val DUE_DATE_REGEX = Regex("""📅\s*(\d{4}-\d{2}-\d{2})""")

    fun parseTasks(
        notePath: String,
        noteTitle: String,
        content: String,
        todayStr: String? = null
    ): List<TaskItem> {
        val lines = content.lines()
        val tasks = mutableListOf<TaskItem>()

        for ((index, line) in lines.withIndex()) {
            val match = TASK_REGEX.matchEntire(line) ?: continue
            val checkMark = match.groupValues[1]
            val isCompleted = checkMark.equals("x", ignoreCase = true)
            val rawText = match.groupValues[2].trim()

            val dueMatch = DUE_DATE_REGEX.find(rawText)
            val dueDate = dueMatch?.groupValues?.get(1)
            val cleanText = rawText.replace(DUE_DATE_REGEX, "").trim()

            var isOverdue = false
            var isDueToday = false

            if (dueDate != null && todayStr != null && !isCompleted) {
                if (dueDate < todayStr) {
                    isOverdue = true
                } else if (dueDate == todayStr) {
                    isDueToday = true
                }
            }

            tasks.add(
                TaskItem(
                    notePath = notePath,
                    noteTitle = noteTitle,
                    lineIndex = index,
                    text = cleanText,
                    isCompleted = isCompleted,
                    dueDate = dueDate,
                    isOverdue = isOverdue,
                    isDueToday = isDueToday
                )
            )
        }

        return tasks
    }
}
