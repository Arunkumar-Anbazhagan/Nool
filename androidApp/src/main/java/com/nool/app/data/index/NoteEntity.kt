package com.nool.app.data.index

data class NoteEntity(
    val rowid: Long = 0,
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
    val tags: String = "", // Comma-separated tags
    val aliases: String = "", // Comma-separated aliases
    val noteType: String = "Note"
)
