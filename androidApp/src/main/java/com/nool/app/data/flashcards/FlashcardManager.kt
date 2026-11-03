package com.nool.app.data.flashcards

import com.nool.app.domain.repository.VaultRepository
import com.nool.core.markdown.CardReviewState
import com.nool.core.markdown.FlashcardItem
import com.nool.core.markdown.FlashcardParser
import com.nool.core.markdown.ReviewGrade
import com.nool.core.markdown.SpacedRepetitionScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FlashcardManager(
    private val vaultRepository: VaultRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getReviewsFile(): File {
        val root = File(vaultRepository.getVaultRootDir())
        val noolDir = File(root, ".nool")
        if (!noolDir.exists()) noolDir.mkdirs()
        return File(noolDir, "reviews.json")
    }

    suspend fun loadReviewStates(): Map<String, CardReviewState> = withContext(Dispatchers.IO) {
        val file = getReviewsFile()
        if (!file.exists()) return@withContext emptyMap()
        try {
            val content = file.readText()
            val json = JSONObject(content)
            val map = mutableMapOf<String, CardReviewState>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val cardId = keys.next()
                val item = json.getJSONObject(cardId)
                map[cardId] = CardReviewState(
                    cardId = cardId,
                    repetition = item.optInt("repetition", 0),
                    intervalDays = item.optInt("intervalDays", 0),
                    easinessFactor = item.optDouble("easinessFactor", 2.5),
                    nextReviewDate = item.optString("nextReviewDate", ""),
                    lastReviewDate = item.optString("lastReviewDate", "")
                )
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun saveReviewStates(states: Map<String, CardReviewState>) = withContext(Dispatchers.IO) {
        val file = getReviewsFile()
        val json = JSONObject()
        // Sort keys for deterministic, diff-friendly output
        for (cardId in states.keys.sorted()) {
            val state = states[cardId] ?: continue
            val item = JSONObject().apply {
                put("repetition", state.repetition)
                put("intervalDays", state.intervalDays)
                put("easinessFactor", state.easinessFactor)
                put("nextReviewDate", state.nextReviewDate)
                put("lastReviewDate", state.lastReviewDate)
            }
            json.put(cardId, item)
        }
        file.writeText(json.toString(2))
    }

    suspend fun getAllFlashcards(): List<FlashcardItem> = withContext(Dispatchers.IO) {
        val allNotes = vaultRepository.getIndexManager().getAllNotes()
        val cards = mutableListOf<FlashcardItem>()
        for (note in allNotes) {
            val parsed = FlashcardParser.parseFlashcards(note.relativePath, note.title, note.content)
            cards.addAll(parsed)
        }
        cards
    }

    suspend fun getDueFlashcards(): List<Pair<FlashcardItem, CardReviewState?>> = withContext(Dispatchers.IO) {
        val allCards = getAllFlashcards()
        val states = loadReviewStates()
        val todayStr = dateFormat.format(Date())

        val due = mutableListOf<Pair<FlashcardItem, CardReviewState?>>()
        for (card in allCards) {
            val state = states[card.cardId]
            if (SpacedRepetitionScheduler.isDue(state, todayStr)) {
                due.add(card to state)
            }
        }
        due
    }

    suspend fun recordReview(
        cardId: String,
        grade: ReviewGrade,
        currentState: CardReviewState?
    ): CardReviewState = withContext(Dispatchers.IO) {
        val todayStr = dateFormat.format(Date())
        val baseState = currentState ?: CardReviewState(cardId = cardId)
        val nextState = SpacedRepetitionScheduler.calculateNextReview(baseState, grade, todayStr)

        val states = loadReviewStates().toMutableMap()
        states[cardId] = nextState
        saveReviewStates(states)

        nextState
    }
}
