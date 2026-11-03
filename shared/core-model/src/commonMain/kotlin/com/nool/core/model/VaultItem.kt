package com.nool.core.model

sealed interface VaultItem {
    val name: String
    val relativePath: String
    val isDirectory: Boolean

    data class NoteItem(
        override val name: String,
        override val relativePath: String,
        val title: String,
        val lastModified: Long,
        val size: Long,
        val excerpt: String = "",
        val isPinned: Boolean = false,
        val isFavorite: Boolean = false,
        val isArchived: Boolean = false,
        val tags: List<String> = emptyList(),
        val noteType: String = "Note",
        val createdTimestamp: Long = 0L
    ) : VaultItem {
        override val isDirectory: Boolean = false
    }

    data class FolderItem(
        override val name: String,
        override val relativePath: String,
        val childCount: Int = 0
    ) : VaultItem {
        override val isDirectory: Boolean = true
    }
}

data class TrashedItem(
    val id: String,
    val originalRelativePath: String,
    val name: String,
    val deletedTimestamp: Long,
    val isDirectory: Boolean
)
