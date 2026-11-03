package com.nool.core.markdown

data class SlashCommand(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val snippet: String,
    val cursorOffset: Int = snippet.length
)

object SlashCommandCatalog {
    val commands = listOf(
        SlashCommand(
            id = "heading1",
            title = "Heading 1",
            description = "Top-level section heading",
            iconName = "H1",
            snippet = "# "
        ),
        SlashCommand(
            id = "heading2",
            title = "Heading 2",
            description = "Medium section heading",
            iconName = "H2",
            snippet = "## "
        ),
        SlashCommand(
            id = "heading3",
            title = "Heading 3",
            description = "Sub-section heading",
            iconName = "H3",
            snippet = "### "
        ),
        SlashCommand(
            id = "table",
            title = "Table",
            description = "3x3 formatted Markdown table",
            iconName = "Table",
            snippet = "| Header 1 | Header 2 | Header 3 |\n| :--- | :---: | ---: |\n| Item 1 | Item 2 | Item 3 |\n| Item 4 | Item 5 | Item 6 |\n"
        ),
        SlashCommand(
            id = "callout_note",
            title = "Callout: Note",
            description = "Informative callout block",
            iconName = "Info",
            snippet = "> [!NOTE] Note\n> Content here...\n"
        ),
        SlashCommand(
            id = "callout_tip",
            title = "Callout: Tip",
            description = "Helpful tip or best practice",
            iconName = "Lightbulb",
            snippet = "> [!TIP] Tip\n> Helpful advice here...\n"
        ),
        SlashCommand(
            id = "callout_warning",
            title = "Callout: Warning",
            description = "Urgent warning or caution",
            iconName = "Warning",
            snippet = "> [!WARNING] Warning\n> Critical warning message...\n"
        ),
        SlashCommand(
            id = "todo",
            title = "To-do Checkbox",
            description = "Interactive task item",
            iconName = "CheckBox",
            snippet = "- [ ] "
        ),
        SlashCommand(
            id = "bullet",
            title = "Bullet List",
            description = "Unordered list item",
            iconName = "List",
            snippet = "- "
        ),
        SlashCommand(
            id = "numbered",
            title = "Numbered List",
            description = "Ordered list item",
            iconName = "FormatListNumbered",
            snippet = "1. "
        ),
        SlashCommand(
            id = "code",
            title = "Code Block",
            description = "Fenced code block with syntax highlighting",
            iconName = "Code",
            snippet = "```kotlin\n\n```\n",
            cursorOffset = 10
        ),
        SlashCommand(
            id = "math",
            title = "Math Block",
            description = "KaTeX LaTeX mathematical equation block",
            iconName = "Functions",
            snippet = "$$\n\n$$\n",
            cursorOffset = 3
        ),
        SlashCommand(
            id = "mermaid",
            title = "Mermaid Diagram",
            description = "Flowchart or sequence diagram",
            iconName = "AccountTree",
            snippet = "```mermaid\ngraph TD\n    A[Start] --> B[Process]\n    B --> C[End]\n```\n"
        ),
        SlashCommand(
            id = "embed",
            title = "Embed Note",
            description = "Transclude another note or section",
            iconName = "FilePresent",
            snippet = "![["
        ),
        SlashCommand(
            id = "footnote",
            title = "Footnote",
            description = "Insert footnote reference and definition",
            iconName = "Notes",
            snippet = "[^1]\n\n[^1]: Footnote text here.\n"
        )
    )

    fun filter(query: String): List<SlashCommand> {
        val trimmed = query.trim().lowercase().removePrefix("/")
        if (trimmed.isEmpty()) return commands
        return commands.filter {
            it.title.lowercase().contains(trimmed) ||
            it.description.lowercase().contains(trimmed) ||
            it.id.lowercase().contains(trimmed)
        }
    }
}
