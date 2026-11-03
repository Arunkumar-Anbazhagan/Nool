package com.nool.core.markdown

enum class ReviewGrade(val value: Int) {
    AGAIN(1),
    HARD(3),
    GOOD(4),
    EASY(5)
}

data class CardReviewState(
    val cardId: String,
    val repetition: Int = 0,
    val intervalDays: Int = 0,
    val easinessFactor: Double = 2.5,
    val nextReviewDate: String = "",
    val lastReviewDate: String = ""
)

object SpacedRepetitionScheduler {

    /**
     * Calculates the next review state using the standard SuperMemo-2 (SM-2) algorithm.
     */
    fun calculateNextReview(
        currentState: CardReviewState,
        grade: ReviewGrade,
        currentDateStr: String
    ): CardReviewState {
        val gradeVal = grade.value
        val newRepetition: Int
        val newInterval: Int
        var newEf: Double

        if (gradeVal < 3) {
            newRepetition = 0
            newInterval = 1
            newEf = currentState.easinessFactor
        } else {
            newRepetition = currentState.repetition + 1
            newInterval = when (currentState.repetition) {
                0 -> 1
                1 -> 6
                else -> (currentState.intervalDays * currentState.easinessFactor).toInt().coerceAtLeast(1)
            }
            newEf = currentState.easinessFactor + (0.1 - (5 - gradeVal) * (0.08 + (5 - gradeVal) * 0.02))
            if (newEf < 1.3) newEf = 1.3
        }

        val nextDateStr = addDaysToDateString(currentDateStr, newInterval)

        return CardReviewState(
            cardId = currentState.cardId,
            repetition = newRepetition,
            intervalDays = newInterval,
            easinessFactor = newEf,
            nextReviewDate = nextDateStr,
            lastReviewDate = currentDateStr
        )
    }

    fun isDue(cardState: CardReviewState?, todayStr: String): Boolean {
        if (cardState == null || cardState.nextReviewDate.isBlank()) return true
        return cardState.nextReviewDate <= todayStr
    }

    private fun addDaysToDateString(dateStr: String, days: Int): String {
        val parts = dateStr.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return dateStr
        var y = parts[0]
        var m = parts[1]
        var d = parts[2] + days

        while (d > daysInMonth(y, m)) {
            d -= daysInMonth(y, m)
            m++
            if (m > 12) {
                m = 1
                y++
            }
        }
        return "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}
