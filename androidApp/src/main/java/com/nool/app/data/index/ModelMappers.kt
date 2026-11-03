package com.nool.app.data.index

import com.nool.core.model.IndexedLink
import com.nool.core.model.IndexedNote

fun NoteEntity.toIndexedNote(): IndexedNote {
    return IndexedNote(
        id = rowid,
        relativePath = relativePath,
        title = title,
        folderPath = folderPath,
        content = content,
        excerpt = excerpt,
        lastModified = lastModified,
        createdTimestamp = createdTimestamp,
        size = size,
        isPinned = isPinned,
        isFavorite = isFavorite,
        isArchived = isArchived,
        tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
        aliases = if (aliases.isBlank()) emptyList() else aliases.split(",").map { it.trim() },
        noteType = noteType
    )
}

fun IndexedNote.toEntity(): NoteEntity {
    return NoteEntity(
        rowid = id,
        relativePath = relativePath,
        title = title,
        folderPath = folderPath,
        content = content,
        excerpt = excerpt,
        lastModified = lastModified,
        createdTimestamp = createdTimestamp,
        size = size,
        isPinned = isPinned,
        isFavorite = isFavorite,
        isArchived = isArchived,
        tags = tags.joinToString(","),
        aliases = aliases.joinToString(","),
        noteType = noteType
    )
}

fun LinkEntity.toIndexedLink(): IndexedLink {
    return IndexedLink(
        id = id,
        sourcePath = sourcePath,
        rawTarget = rawTarget,
        targetTitle = targetTitle,
        targetHeading = targetHeading,
        alias = alias,
        isEmbed = isEmbed,
        resolvedPath = resolvedPath
    )
}

fun IndexedLink.toEntity(): LinkEntity {
    return LinkEntity(
        id = id,
        sourcePath = sourcePath,
        rawTarget = rawTarget,
        targetTitle = targetTitle,
        targetHeading = targetHeading,
        alias = alias,
        isEmbed = isEmbed,
        resolvedPath = resolvedPath
    )
}
