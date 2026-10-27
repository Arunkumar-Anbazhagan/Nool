package com.nool.app.data.index

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes_index",
    indices = [Index(value = ["relativePath"], unique = true)]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid")
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

@Entity(tableName = "notes_fts")
@Fts4(contentEntity = NoteEntity::class)
data class NoteFtsEntity(
    val title: String,
    val content: String,
    val tags: String,
    val folderPath: String
)
