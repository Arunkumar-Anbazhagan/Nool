package com.nool.core.model

data class IndexedNote(
    val id: Long = 0,
    val relativePath: String,
    val title: String,
    val folderPath: String,
    val content: String,
    val excerpt: String,
    val lastModified: Long,
    val createdTimestamp: Long,
    val size: Long,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val tags: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val noteType: String = "Note"
)

data class IndexedLink(
    val id: Long = 0,
    val sourcePath: String,
    val rawTarget: String,
    val targetTitle: String,
    val targetHeading: String? = null,
    val alias: String? = null,
    val isEmbed: Boolean = false,
    val resolvedPath: String? = null
)

data class BacklinkInfo(
    val sourcePath: String,
    val sourceTitle: String,
    val snippet: String,
    val alias: String? = null,
    val isEmbed: Boolean = false
)

data class UnlinkedMentionInfo(
    val sourcePath: String,
    val sourceTitle: String,
    val matchedText: String,
    val snippet: String
)

enum class NoteSearchScope {
    ALL,
    PINNED,
    FAVORITES,
    ARCHIVED
}
