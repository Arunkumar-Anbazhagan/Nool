package com.nool.core.markdown

enum class FlashcardType {
    BASIC,
    CLOZE
}

data class FlashcardItem(
    val cardId: String,
    val notePath: String,
    val noteTitle: String,
    val lineIndex: Int,
    val type: FlashcardType,
    val front: String,
    val back: String
)

object FlashcardParser {

    private val CLOZE_HIGHLIGHT_REGEX = Regex("""==([^=]+)==""")
    private val CLOZE_ANKI_REGEX = Regex("""\{\{c\d+::([^}]+)\}\}""")

    fun parseFlashcards(
        notePath: String,
        noteTitle: String,
        content: String
    ): List<FlashcardItem> {
        val lines = content.lines()
        val cards = mutableListOf<FlashcardItem>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            // 1. Check Cloze Deletion (==text== or {{c1::text}})
            val clozeMatch = CLOZE_HIGHLIGHT_REGEX.find(line) ?: CLOZE_ANKI_REGEX.find(line)
            if (clozeMatch != null && !line.startsWith("#")) {
                val hiddenText = clozeMatch.groupValues[1].trim()
                val prompt = line.replace(clozeMatch.value, "[...]")
                val cardId = generateCardId(notePath, i, prompt)
                cards.add(
                    FlashcardItem(
                        cardId = cardId,
                        notePath = notePath,
                        noteTitle = noteTitle,
                        lineIndex = i,
                        type = FlashcardType.CLOZE,
                        front = prompt,
                        back = hiddenText
                    )
                )
                i++
                continue
            }

            // 2. Check Q:: A inline card
            if (line.contains("::") && !line.startsWith("#") && !line.startsWith("http") && !line.contains("{{c")) {
                val front = line.substringBefore("::").trim()
                val back = line.substringAfter("::").trim()
                if (front.isNotEmpty() && back.isNotEmpty()) {
                    val cardId = generateCardId(notePath, i, front)
                    cards.add(
                        FlashcardItem(
                            cardId = cardId,
                            notePath = notePath,
                            noteTitle = noteTitle,
                            lineIndex = i,
                            type = FlashcardType.BASIC,
                            front = front,
                            back = back
                        )
                    )
                    i++
                    continue
                }
            }

            // 3. Check multi-line card: Front \n ? \n Back
            if (line == "?" && i > 0 && i + 1 < lines.size) {
                val front = lines[i - 1].trim()
                val back = lines[i + 1].trim()
                if (front.isNotEmpty() && back.isNotEmpty() && !front.startsWith("#")) {
                    val cardId = generateCardId(notePath, i - 1, front)
                    cards.add(
                        FlashcardItem(
                            cardId = cardId,
                            notePath = notePath,
                            noteTitle = noteTitle,
                            lineIndex = i - 1,
                            type = FlashcardType.BASIC,
                            front = front,
                            back = back
                        )
                    )
                    i += 2
                    continue
                }
            }

            i++
        }

        return cards
    }

    private fun generateCardId(notePath: String, lineIndex: Int, prompt: String): String {
        val raw = "$notePath:$lineIndex:$prompt"
        return raw.hashCode().toString(16)
    }
}
