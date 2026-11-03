package com.nool.app.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object MarkdownFormattingHelper {

    fun wrapSelection(
        current: TextFieldValue,
        prefix: String,
        suffix: String = prefix,
        placeholder: String = ""
    ): TextFieldValue {
        val text = current.text
        val selection = current.selection

        return if (selection.collapsed) {
            val insertText = "$prefix$placeholder$suffix"
            val newText = text.replaceRange(selection.start, selection.end, insertText)
            val newCursor = selection.start + prefix.length
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor, newCursor + placeholder.length)
            )
        } else {
            val selected = text.substring(selection.start, selection.end)
            val isWrapped = selected.startsWith(prefix) && selected.endsWith(suffix) && selected.length >= prefix.length + suffix.length

            if (isWrapped) {
                // Unwrap
                val unwrapped = selected.removePrefix(prefix).removeSuffix(suffix)
                val newText = text.replaceRange(selection.start, selection.end, unwrapped)
                TextFieldValue(
                    text = newText,
                    selection = TextRange(selection.start, selection.start + unwrapped.length)
                )
            } else {
                // Wrap
                val wrapped = "$prefix$selected$suffix"
                val newText = text.replaceRange(selection.start, selection.end, wrapped)
                TextFieldValue(
                    text = newText,
                    selection = TextRange(selection.start, selection.start + wrapped.length)
                )
            }
        }
    }

    fun toggleBold(current: TextFieldValue): TextFieldValue =
        wrapSelection(current, "**", "**", "bold text")

    fun toggleItalic(current: TextFieldValue): TextFieldValue =
        wrapSelection(current, "*", "*", "italic text")

    fun toggleStrikethrough(current: TextFieldValue): TextFieldValue =
        wrapSelection(current, "~~", "~~", "strikethrough")

    fun toggleInlineCode(current: TextFieldValue): TextFieldValue =
        wrapSelection(current, "`", "`", "code")

    fun insertWikiLink(current: TextFieldValue): TextFieldValue =
        wrapSelection(current, "[[", "]]", "")

    fun insertCodeBlock(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val selected = if (selection.collapsed) "" else text.substring(selection.start, selection.end)
        val snippet = "\n```\n${if (selected.isEmpty()) "code" else selected}\n```\n"
        val newText = text.replaceRange(selection.start, selection.end, snippet)
        val cursor = selection.start + 5
        return TextFieldValue(newText, TextRange(cursor, cursor + (if (selected.isEmpty()) 4 else selected.length)))
    }

    fun cycleHeading(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
        val lineEnd = text.indexOf('\n', selection.start).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val newLine = when {
            currentLine.startsWith("### ") -> currentLine.replaceFirst("### ", "")
            currentLine.startsWith("## ") -> currentLine.replaceFirst("## ", "### ")
            currentLine.startsWith("# ") -> currentLine.replaceFirst("# ", "## ")
            else -> "# $currentLine"
        }

        val newText = text.replaceRange(lineStart, lineEnd, newLine)
        val shift = newLine.length - currentLine.length
        return TextFieldValue(newText, TextRange(selection.start + shift, selection.end + shift))
    }

    fun toggleLinePrefix(current: TextFieldValue, prefix: String): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
        val lineEnd = text.indexOf('\n', selection.start).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val (newLine, shift) = if (currentLine.startsWith(prefix)) {
            currentLine.removePrefix(prefix) to -prefix.length
        } else {
            "$prefix$currentLine" to prefix.length
        }

        val newText = text.replaceRange(lineStart, lineEnd, newLine)
        return TextFieldValue(newText, TextRange((selection.start + shift).coerceAtLeast(lineStart), (selection.end + shift).coerceAtLeast(lineStart)))
    }

    fun toggleBulletList(current: TextFieldValue): TextFieldValue =
        toggleLinePrefix(current, "- ")

    fun toggleNumberedList(current: TextFieldValue): TextFieldValue =
        toggleLinePrefix(current, "1. ")

    fun toggleChecklist(current: TextFieldValue): TextFieldValue =
        toggleLinePrefix(current, "- [ ] ")

    fun toggleBlockquote(current: TextFieldValue): TextFieldValue =
        toggleLinePrefix(current, "> ")

    fun insertTable(current: TextFieldValue): TextFieldValue {
        val tableSnippet = """
| Header 1 | Header 2 |
| --- | --- |
| Item 1 | Item 2 |
""".trimIndent()
        val text = current.text
        val selection = current.selection
        val prefix = if (selection.start > 0 && text[selection.start - 1] != '\n') "\n\n" else "\n"
        val snippet = "$prefix$tableSnippet\n"
        val newText = text.replaceRange(selection.start, selection.end, snippet)
        val cursor = selection.start + snippet.length
        return TextFieldValue(newText, TextRange(cursor, cursor))
    }

    fun insertLink(current: TextFieldValue, label: String = "link text", url: String = "https://"): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val selected = if (selection.collapsed) label else text.substring(selection.start, selection.end)
        val snippet = "[$selected]($url)"
        val newText = text.replaceRange(selection.start, selection.end, snippet)
        val cursorStart = selection.start + selected.length + 3
        val cursorEnd = cursorStart + url.length
        return TextFieldValue(newText, TextRange(cursorStart, cursorEnd))
    }

    fun insertAttachment(current: TextFieldValue, alt: String, relativePath: String): TextFieldValue {
        val snippet = "![$alt]($relativePath)\n"
        val text = current.text
        val selection = current.selection
        val newText = text.replaceRange(selection.start, selection.end, snippet)
        val cursor = selection.start + snippet.length
        return TextFieldValue(newText, TextRange(cursor, cursor))
    }

    fun insertCallout(
        current: TextFieldValue,
        type: String = "note",
        title: String = "",
        collapsible: String = ""
    ): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val selected = if (selection.collapsed) "Callout text here" else text.substring(selection.start, selection.end)
        val titlePart = if (title.isNotBlank()) " $title" else ""
        val snippet = "> [!$type]$collapsible$titlePart\n> $selected\n"
        val prefix = if (selection.start > 0 && text[selection.start - 1] != '\n') "\n\n" else "\n"
        val finalSnippet = "$prefix$snippet"
        val newText = text.replaceRange(selection.start, selection.end, finalSnippet)
        val cursor = selection.start + finalSnippet.length
        return TextFieldValue(newText, TextRange(cursor, cursor))
    }

    /**
     * Smart continuation for list items, checklists, numbered lists, and quotes when Enter is pressed.
     * When pressed on an empty item, it terminates the list by removing the empty prefix.
     */
    fun handleSmartEnter(oldVal: TextFieldValue, newVal: TextFieldValue): TextFieldValue? {
        val oldText = oldVal.text
        val newText = newVal.text

        // Only handle when characters were inserted and a newline was just typed
        if (newText.length <= oldText.length) return null
        val cursor = newVal.selection.start
        if (cursor <= 0 || newText[cursor - 1] != '\n') return null

        // Ensure exactly one character (\n) was inserted at cursor - 1
        val charsAdded = newText.length - oldText.length + oldVal.selection.length
        if (charsAdded != 1) return null

        val prevLineEnd = cursor - 1
        val prevLineStart = newText.lastIndexOf('\n', prevLineEnd - 1) + 1
        val prevLine = newText.substring(prevLineStart, prevLineEnd)

        val indent = prevLine.takeWhile { it == ' ' || it == '\t' }

        // 1. Checklist: "- [ ] " or "- [x] " or "- [X] "
        val checklistMatch = Regex("""^(\s*)-\s*\[([ xX])\]\s*(.*)$""").find(prevLine)
        if (checklistMatch != null) {
            val content = checklistMatch.groupValues[3]
            return if (content.isBlank()) {
                val clearedText = newText.removeRange(prevLineStart, cursor)
                TextFieldValue(clearedText, TextRange(prevLineStart))
            } else {
                val prefix = "$indent- [ ] "
                val updatedText = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                val newCursor = cursor + prefix.length
                TextFieldValue(updatedText, TextRange(newCursor))
            }
        }

        // 2. Numbered list: "1. "
        val numberedMatch = Regex("""^(\s*)(\d+)\.\s*(.*)$""").find(prevLine)
        if (numberedMatch != null) {
            val num = numberedMatch.groupValues[2].toIntOrNull() ?: 1
            val content = numberedMatch.groupValues[3]
            return if (content.isBlank()) {
                val clearedText = newText.removeRange(prevLineStart, cursor)
                TextFieldValue(clearedText, TextRange(prevLineStart))
            } else {
                val prefix = "$indent${num + 1}. "
                val updatedText = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                val newCursor = cursor + prefix.length
                TextFieldValue(updatedText, TextRange(newCursor))
            }
        }

        // 3. Bullet list: "- " or "* " or "+ "
        val bulletMatch = Regex("""^(\s*)([-*+])\s*(.*)$""").find(prevLine)
        if (bulletMatch != null) {
            val bullet = bulletMatch.groupValues[2]
            val content = bulletMatch.groupValues[3]
            return if (content.isBlank()) {
                val clearedText = newText.removeRange(prevLineStart, cursor)
                TextFieldValue(clearedText, TextRange(prevLineStart))
            } else {
                val prefix = "$indent$bullet "
                val updatedText = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                val newCursor = cursor + prefix.length
                TextFieldValue(updatedText, TextRange(newCursor))
            }
        }

        // 4. Blockquote: "> "
        val quoteMatch = Regex("""^(\s*)>\s*(.*)$""").find(prevLine)
        if (quoteMatch != null) {
            val content = quoteMatch.groupValues[2]
            return if (content.isBlank()) {
                val clearedText = newText.removeRange(prevLineStart, cursor)
                TextFieldValue(clearedText, TextRange(prevLineStart))
            } else {
                val prefix = "$indent> "
                val updatedText = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                val newCursor = cursor + prefix.length
                TextFieldValue(updatedText, TextRange(newCursor))
            }
        }

        return null
    }
}
