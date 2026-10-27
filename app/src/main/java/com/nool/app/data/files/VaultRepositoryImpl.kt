package com.nool.app.data.files

import com.nool.app.data.index.FrontmatterAndTagParser
import com.nool.app.data.index.IndexManager
import com.nool.app.data.index.NoteEntity
import com.nool.app.data.index.SearchScope
import com.nool.app.domain.model.TrashedItem
import com.nool.app.domain.model.VaultItem
import com.nool.app.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.InputStream

class VaultRepositoryImpl(
    private val fileManager: FileManager,
    private val indexManager: IndexManager
) : VaultRepository {

    private val _vaultUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val vaultUpdates: Flow<Unit> = _vaultUpdates.asSharedFlow()

    private fun notifyChanged() {
        _vaultUpdates.tryEmit(Unit)
    }

    override suspend fun listItems(folderRelativePath: String): Result<List<VaultItem>> = runCatching {
        fileManager.listItems(folderRelativePath)
    }

    override suspend fun createNote(
        folderRelativePath: String,
        title: String,
        content: String
    ): Result<VaultItem.NoteItem> = runCatching {
        val note = fileManager.createNote(folderRelativePath, title, content)
        indexManager.indexFile(File(fileManager.vaultRoot, note.relativePath))
        notifyChanged()
        note
    }

    override suspend fun createFolder(parentRelativePath: String, name: String): Result<VaultItem.FolderItem> =
        runCatching {
            val folder = fileManager.createFolder(parentRelativePath, name)
            notifyChanged()
            folder
        }

    override suspend fun readNoteContent(relativePath: String): Result<String> = runCatching {
        fileManager.readNoteContent(relativePath)
    }

    override suspend fun saveNoteContent(relativePath: String, content: String): Result<Unit> = runCatching {
        fileManager.saveNoteContent(relativePath, content)
        indexManager.indexFile(File(fileManager.vaultRoot, relativePath))
        notifyChanged()
    }

    override suspend fun renameItem(relativePath: String, newName: String): Result<String> = runCatching {
        val oldFile = File(fileManager.vaultRoot, relativePath)
        val isNote = oldFile.isFile && oldFile.name.endsWith(".md", ignoreCase = true)

        indexManager.removeFile(relativePath)
        val newPath = fileManager.renameItem(relativePath, newName)
        val target = File(fileManager.vaultRoot, newPath)
        if (target.isFile) {
            indexManager.indexFile(target)
            if (isNote) {
                indexManager.updateLinksOnNoteRename(relativePath, newPath)
            }
        } else {
            // Re-index folder items
            target.walkTopDown().filter { it.isFile && it.name.endsWith(".md") }.forEach {
                indexManager.indexFile(it)
            }
        }
        notifyChanged()
        newPath
    }

    override suspend fun deleteItem(relativePath: String): Result<Unit> = runCatching {
        indexManager.removeFile(relativePath)
        fileManager.deleteItem(relativePath)
        notifyChanged()
    }

    override suspend fun moveItem(sourceRelativePath: String, targetFolderRelativePath: String): Result<String> =
        runCatching {
            val oldFile = File(fileManager.vaultRoot, sourceRelativePath)
            val isNote = oldFile.isFile && oldFile.name.endsWith(".md", ignoreCase = true)

            indexManager.removeFile(sourceRelativePath)
            val newPath = fileManager.moveItem(sourceRelativePath, targetFolderRelativePath)
            val target = File(fileManager.vaultRoot, newPath)
            if (target.isFile) {
                indexManager.indexFile(target)
                if (isNote) {
                    indexManager.updateLinksOnNoteRename(sourceRelativePath, newPath)
                }
            } else {
                target.walkTopDown().filter { it.isFile && it.name.endsWith(".md") }.forEach {
                    indexManager.indexFile(it)
                }
            }
            notifyChanged()
            newPath
        }

    override suspend fun getAllFolders(): Result<List<String>> = runCatching {
        fileManager.getAllFolders()
    }

    override fun getVaultRootDir(): String {
        return fileManager.vaultRoot.absolutePath
    }

    override suspend fun setNoteFlag(relativePath: String, flagKey: String, value: Boolean): Result<Unit> = runCatching {
        val currentContent = fileManager.readNoteContent(relativePath)
        val updated = FrontmatterAndTagParser.updateFrontmatterFlag(currentContent, flagKey, value)
        fileManager.saveNoteContent(relativePath, updated)
        indexManager.indexFile(File(fileManager.vaultRoot, relativePath))
        notifyChanged()
    }

    override fun getIndexManager(): IndexManager = indexManager

    override suspend fun searchNotes(
        query: String,
        folder: String?,
        tag: String?,
        type: String?,
        scope: SearchScope
    ): Result<List<NoteEntity>> = runCatching {
        indexManager.search(query, folder, tag, type, scope)
    }

    override suspend fun getAllTags(): Result<Map<String, Int>> = runCatching {
        indexManager.getAllTagsWithCounts()
    }

    override suspend fun rebuildIndex(): Result<Int> = runCatching {
        val count = indexManager.rebuildIndex()
        notifyChanged()
        count
    }

    override suspend fun moveToTrash(relativePath: String): Result<TrashedItem> = runCatching {
        indexManager.removeFile(relativePath)
        val trashed = fileManager.moveToTrash(relativePath)
        notifyChanged()
        trashed
    }

    override suspend fun listTrash(): Result<List<TrashedItem>> = runCatching {
        fileManager.listTrash()
    }

    override suspend fun restoreFromTrash(trashedId: String): Result<String> = runCatching {
        val restoredPath = fileManager.restoreFromTrash(trashedId)
        val file = File(fileManager.vaultRoot, restoredPath)
        if (file.isFile) {
            indexManager.indexFile(file)
        } else {
            file.walkTopDown().filter { it.isFile && it.name.endsWith(".md") }.forEach {
                indexManager.indexFile(it)
            }
        }
        notifyChanged()
        restoredPath
    }

    override suspend fun permanentlyDeleteFromTrash(trashedId: String): Result<Unit> = runCatching {
        fileManager.permanentlyDeleteFromTrash(trashedId)
        notifyChanged()
    }

    override suspend fun emptyTrash(): Result<Unit> = runCatching {
        fileManager.emptyTrash()
        notifyChanged()
    }

    override suspend fun saveAttachment(fileName: String, inputStream: InputStream): Result<String> = runCatching {
        val path = fileManager.saveAttachment(fileName, inputStream)
        notifyChanged()
        path
    }

    override fun getAttachmentFile(relativePath: String): File {
        return fileManager.getAttachmentFile(relativePath)
    }

    override suspend fun getBacklinks(noteRelativePath: String): Result<List<com.nool.app.data.index.BacklinkItem>> = runCatching {
        indexManager.getBacklinks(noteRelativePath)
    }

    override suspend fun getUnlinkedMentions(noteRelativePath: String): Result<List<com.nool.app.data.index.UnlinkedMentionItem>> = runCatching {
        indexManager.getUnlinkedMentions(noteRelativePath)
    }

    override suspend fun convertUnlinkedMention(
        sourcePath: String,
        mentionText: String,
        targetNoteTitle: String
    ): Result<Boolean> = runCatching {
        val success = indexManager.convertUnlinkedMention(sourcePath, mentionText, targetNoteTitle)
        if (success) notifyChanged()
        success
    }

    override suspend fun getNoteCatalog(): Result<List<com.nool.app.data.index.NoteTitleAndPath>> = runCatching {
        indexManager.getNoteCatalog()
    }

    override suspend fun resolveWikiLink(rawTarget: String, sourceNotePath: String): Result<String?> = runCatching {
        val catalog = indexManager.getNoteCatalog()
        com.nool.app.data.index.LinkResolver.resolveLink(rawTarget, sourceNotePath, catalog)
    }

    override suspend fun getGraphData(
        localNotePath: String?,
        localDepth: Int
    ): Result<com.nool.app.domain.model.GraphData> = runCatching {
        indexManager.getGraphData(localNotePath, localDepth)
    }
}
