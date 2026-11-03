package com.nool.app.domain.repository

import com.nool.app.data.index.IndexManager
import com.nool.app.data.index.NoteEntity
import com.nool.app.data.index.SearchScope
import com.nool.core.model.TrashedItem
import com.nool.core.model.VaultItem
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.InputStream

interface VaultRepository {
    val vaultUpdates: Flow<Unit>
    
    suspend fun listItems(folderRelativePath: String): Result<List<VaultItem>>
    suspend fun createNote(folderRelativePath: String, title: String, content: String = ""): Result<VaultItem.NoteItem>
    suspend fun createFolder(parentRelativePath: String, name: String): Result<VaultItem.FolderItem>
    suspend fun readNoteContent(relativePath: String): Result<String>
    suspend fun saveNoteContent(relativePath: String, content: String): Result<Unit>
    suspend fun renameItem(relativePath: String, newName: String): Result<String>
    suspend fun deleteItem(relativePath: String): Result<Unit>
    suspend fun moveItem(sourceRelativePath: String, targetFolderRelativePath: String): Result<String>
    suspend fun getAllFolders(): Result<List<String>>
    fun getVaultRootDir(): String

    // Frontmatter flags (Pin, Favorite, Archive)
    suspend fun setNoteFlag(relativePath: String, flagKey: String, value: Boolean): Result<Unit>

    // Search and Indexing
    fun getIndexManager(): IndexManager
    suspend fun searchNotes(
        query: String,
        folder: String? = null,
        tag: String? = null,
        type: String? = null,
        scope: SearchScope = SearchScope.ALL
    ): Result<List<NoteEntity>>
    suspend fun getAllTags(): Result<Map<String, Int>>
    suspend fun rebuildIndex(): Result<Int>

    // Trash management
    suspend fun moveToTrash(relativePath: String): Result<TrashedItem>
    suspend fun listTrash(): Result<List<TrashedItem>>
    suspend fun restoreFromTrash(trashedId: String): Result<String>
    suspend fun permanentlyDeleteFromTrash(trashedId: String): Result<Unit>
    suspend fun emptyTrash(): Result<Unit>

    // Attachments
    suspend fun saveAttachment(fileName: String, inputStream: InputStream): Result<String>
    fun getAttachmentFile(relativePath: String): File

    // Links, Backlinks & Mentions
    suspend fun getBacklinks(noteRelativePath: String): Result<List<com.nool.app.data.index.BacklinkItem>>
    suspend fun getUnlinkedMentions(noteRelativePath: String): Result<List<com.nool.app.data.index.UnlinkedMentionItem>>
    suspend fun convertUnlinkedMention(sourcePath: String, mentionText: String, targetNoteTitle: String): Result<Boolean>
    suspend fun getNoteCatalog(): Result<List<com.nool.app.data.index.NoteTitleAndPath>>
    suspend fun resolveWikiLink(rawTarget: String, sourceNotePath: String): Result<String?>
    suspend fun getGraphData(localNotePath: String? = null, localDepth: Int = 1): Result<com.nool.core.model.GraphData>
}
