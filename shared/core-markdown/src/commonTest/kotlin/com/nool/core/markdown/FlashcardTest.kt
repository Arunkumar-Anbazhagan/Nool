package com.nool.core.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlashcardTest {

    @Test
    fun testParseQDoubleColonA() {
        val content = """
            # Flashcards
            What is the capital of Japan?:: Tokyo
            What is 2 + 2?:: 4
        """.trimIndent()

        val cards = FlashcardParser.parseFlashcards("notes/quiz.md", "Quiz", content)
        assertEquals(2, cards.size)
        assertEquals("What is the capital of Japan?", cards[0].front)
        assertEquals("Tokyo", cards[0].back)
        assertEquals(FlashcardType.BASIC, cards[0].type)
    }

    @Test
    fun testParseClozeDeletions() {
        val content = """
            The speed of light is ==299,792,458 m/s== in vacuum.
            {{c1::Python}} was created by Guido van Rossum.
        """.trimIndent()

        val cards = FlashcardParser.parseFlashcards("notes/physics.md", "Physics", content)
        assertEquals(2, cards.size)
        assertEquals("The speed of light is [...] in vacuum.", cards[0].front)
        assertEquals("299,792,458 m/s", cards[0].back)
        assertEquals(FlashcardType.CLOZE, cards[0].type)

        assertEquals("[...] was created by Guido van Rossum.", cards[1].front)
        assertEquals("Python", cards[1].back)
    }

    @Test
    fun testParseMultilineCards() {
        val content = """
            Front question
            ?
            Back answer
        """.trimIndent()

        val cards = FlashcardParser.parseFlashcards("notes/cards.md", "Cards", content)
        assertEquals(1, cards.size)
        assertEquals("Front question", cards[0].front)
        assertEquals("Back answer", cards[0].back)
    }

    @Test
    fun testSm2AlgorithmProgression() {
        val initial = CardReviewState(cardId = "card-1")
        val today = "2026-11-04"

        // First review: Good (grade 4)
        val review1 = SpacedRepetitionScheduler.calculateNextReview(initial, ReviewGrade.GOOD, today)
        assertEquals(1, review1.repetition)
        assertEquals(1, review1.intervalDays)
        assertEquals("2026-11-05", review1.nextReviewDate)

        // Second review: Good (grade 4)
        val review2 = SpacedRepetitionScheduler.calculateNextReview(review1, ReviewGrade.GOOD, "2026-11-05")
        assertEquals(2, review2.repetition)
        assertEquals(6, review2.intervalDays)
        assertEquals("2026-11-11", review2.nextReviewDate)

        // Third review: Easy (grade 5)
        val review3 = SpacedRepetitionScheduler.calculateNextReview(review2, ReviewGrade.EASY, "2026-11-11")
        assertEquals(3, review3.repetition)
        assertTrue(review3.intervalDays >= 15) // 6 * 2.5 = 15
        assertTrue(review3.easinessFactor > 2.5) // EF increased

        // Fourth review: Fail / Again (grade 1)
        val review4 = SpacedRepetitionScheduler.calculateNextReview(review3, ReviewGrade.AGAIN, "2026-11-26")
        assertEquals(0, review4.repetition) // Reset to 0
        assertEquals(1, review4.intervalDays) // Reset to 1 day
        assertEquals("2026-11-27", review4.nextReviewDate)
    }
}
