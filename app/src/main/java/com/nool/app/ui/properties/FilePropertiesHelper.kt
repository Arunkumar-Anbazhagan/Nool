package com.nool.app.ui.properties

enum class PropertyType {
    TEXT,
    NUMBER,
    BOOLEAN,
    DATE,
    TAGS,
    ALIASES
}

data class PropertyItem(
    val key: String,
    val value: String,
    val type: PropertyType = PropertyType.TEXT
)

object FilePropertiesHelper {

    fun parseProperties(content: String): Pair<List<PropertyItem>, String> {
        val lines = content.lines()
        if (lines.isEmpty() || lines[0].trim() != "---") {
            return emptyList<PropertyItem>() to content
        }

        var closingIndex = -1
        for (i in 1 until lines.size) {
            if (lines[i].trim() == "---") {
                closingIndex = i
                break
            }
        }

        if (closingIndex == -1) {
            return emptyList<PropertyItem>() to content
        }

        val properties = mutableListOf<PropertyItem>()
        var currentKey = ""
        val currentListItems = mutableListOf<String>()

        fun flushList() {
            if (currentKey.isNotEmpty()) {
                val listType = if (currentKey.equals("tags", true)) PropertyType.TAGS else PropertyType.ALIASES
                properties.add(PropertyItem(currentKey, currentListItems.joinToString(", "), listType))
                currentKey = ""
                currentListItems.clear()
            }
        }

        for (i in 1 until closingIndex) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            if (line.startsWith("- ")) {
                val item = line.removePrefix("- ").trim().removeSurrounding("\"").removeSurrounding("'")
                currentListItems.add(item)
            } else if (line.contains(":")) {
                flushList()
                val k = line.substringBefore(":").trim()
                val v = line.substringAfter(":").trim().removeSurrounding("\"").removeSurrounding("'")

                val detectedType = when {
                    k.equals("pinned", true) || k.equals("favorite", true) || k.equals("archived", true) || v.equals("true", true) || v.equals("false", true) -> PropertyType.BOOLEAN
                    k.equals("tags", true) -> PropertyType.TAGS
                    k.equals("aliases", true) || k.equals("alias", true) -> PropertyType.ALIASES
                    v.toIntOrNull() != null || v.toDoubleOrNull() != null -> PropertyType.NUMBER
                    v.matches(Regex("""\d{4}-\d{2}-\d{2}""")) -> PropertyType.DATE
                    else -> PropertyType.TEXT
                }

                if (v.isEmpty() && (k.equals("tags", true) || k.equals("aliases", true))) {
                    currentKey = k
                } else {
                    properties.add(PropertyItem(k, v, detectedType))
                }
            }
        }
        flushList()

        val bodyLines = if (closingIndex + 1 < lines.size) lines.subList(closingIndex + 1, lines.size) else emptyList()
        val bodyText = bodyLines.joinToString("\n").trimStart('\n')

        return properties to bodyText
    }

    fun updateProperties(currentContent: String, newProperties: List<PropertyItem>): String {
        val (_, body) = parseProperties(currentContent)
        if (newProperties.isEmpty()) {
            return body
        }

        val sb = StringBuilder()
        sb.append("---\n")
        for (prop in newProperties) {
            val key = prop.key.trim()
            val value = prop.value.trim()
            when (prop.type) {
                PropertyType.TAGS, PropertyType.ALIASES -> {
                    val items = value.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
                    if (items.isNotEmpty()) {
                        sb.append("$key:\n")
                        for (item in items) {
                            sb.append("  - $item\n")
                        }
                    } else {
                        sb.append("$key: []\n")
                    }
                }
                PropertyType.BOOLEAN -> {
                    sb.append("$key: ${value.equals("true", true)}\n")
                }
                PropertyType.NUMBER -> {
                    sb.append("$key: $value\n")
                }
                else -> {
                    if (value.contains(":") || value.contains("#") || value.contains("\n")) {
                        sb.append("$key: \"${value.replace("\"", "\\\"")}\"\n")
                    } else {
                        sb.append("$key: $value\n")
                    }
                }
            }
        }
        sb.append("---\n\n")
        sb.append(body)
        return sb.toString()
    }
}
