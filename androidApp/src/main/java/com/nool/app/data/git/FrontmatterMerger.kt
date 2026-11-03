package com.nool.app.data.git

object FrontmatterMerger {

    data class MergeOutput(
        val isClean: Boolean,
        val mergedText: String,
        val conflictingKeys: List<String> = emptyList(),
        val bodyConflict: Boolean = false
    )

    private val FRONTMATTER_REGEX = Regex("""^---\r?\n([\s\S]*?)\r?\n---\r?\n?([\s\S]*)$""")

    fun parseFrontmatterAndBody(content: String): Pair<Map<String, String>, String> {
        val match = FRONTMATTER_REGEX.matchEntire(content.trimStart())
        if (match == null) {
            return emptyMap<String, String>() to content
        }

        val rawYaml = match.groupValues[1]
        val body = match.groupValues[2]

        val props = mutableMapOf<String, String>()
        rawYaml.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains(":")) {
                val key = trimmed.substringBefore(":").trim()
                val value = trimmed.substringAfter(":").trim()
                props[key] = value
            }
        }
        return props to body
    }

    /**
     * Attempts an intelligent 3-way merge on Markdown files with YAML frontmatter.
     */
    fun merge(baseContent: String, localContent: String, remoteContent: String): MergeOutput {
        val (baseProps, baseBody) = parseFrontmatterAndBody(baseContent)
        val (localProps, localBody) = parseFrontmatterAndBody(localContent)
        val (remoteProps, remoteBody) = parseFrontmatterAndBody(remoteContent)

        // 1. Merge frontmatter property by property
        val mergedProps = mutableMapOf<String, String>()
        val conflictingKeys = mutableListOf<String>()

        val allKeys = baseProps.keys + localProps.keys + remoteProps.keys

        for (key in allKeys) {
            val baseVal = baseProps[key]
            val localVal = localProps[key]
            val remoteVal = remoteProps[key]

            when {
                // Both agreed on same change
                localVal == remoteVal -> {
                    if (localVal != null) mergedProps[key] = localVal
                }
                // Only local changed
                localVal != baseVal && remoteVal == baseVal -> {
                    if (localVal != null) mergedProps[key] = localVal
                }
                // Only remote changed
                remoteVal != baseVal && localVal == baseVal -> {
                    if (remoteVal != null) mergedProps[key] = remoteVal
                }
                // Both changed differently: Conflict on this property!
                else -> {
                    conflictingKeys.add(key)
                    // Keep local value as temporary fallback
                    if (localVal != null) mergedProps[key] = localVal
                }
            }
        }

        // 2. Body comparison
        val isBodyClean = (localBody.trim() == remoteBody.trim()) ||
                (localBody.trim() == baseBody.trim() && remoteBody.isNotBlank()) ||
                (remoteBody.trim() == baseBody.trim() && localBody.isNotBlank())

        val resolvedBody = when {
            localBody.trim() == remoteBody.trim() -> localBody
            localBody.trim() == baseBody.trim() -> remoteBody
            remoteBody.trim() == baseBody.trim() -> localBody
            else -> localBody // body conflict
        }

        val frontmatterClean = conflictingKeys.isEmpty()
        val isClean = frontmatterClean && isBodyClean

        // Reconstruct merged document
        val sb = StringBuilder()
        if (mergedProps.isNotEmpty()) {
            sb.append("---\n")
            for ((k, v) in mergedProps) {
                sb.append("$k: $v\n")
            }
            sb.append("---\n\n")
        }
        sb.append(resolvedBody)

        return MergeOutput(
            isClean = isClean,
            mergedText = sb.toString(),
            conflictingKeys = conflictingKeys,
            bodyConflict = !isBodyClean
        )
    }
}
