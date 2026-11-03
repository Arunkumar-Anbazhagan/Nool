package com.nool.app.data.files

import android.content.Context
import com.nool.core.model.TrashedItem
import com.nool.core.model.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class FileManager(
    private val context: Context? = null,
    customVaultDir: File? = null
) {
    val vaultRoot: File = customVaultDir ?: File(
        requireNotNull(context) { "Context must be provided if customVaultDir is not specified." }.filesDir,
        "vault"
    )

    val trashDir: File = File(vaultRoot, ".trash")
    val attachmentsDir: File = File(vaultRoot, "attachments")
    private val trashManifestFile: File get() = File(trashDir, "manifest.json")

    suspend fun ensureVaultInitialized(): Unit = withContext(Dispatchers.IO) {
        if (!vaultRoot.exists()) {
            vaultRoot.mkdirs()
        }
        if (!trashDir.exists()) {
            trashDir.mkdirs()
        }
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
        }
        val welcomeFile = File(vaultRoot, "Welcome to Nool.md")
        val hasUserFiles = vaultRoot.listFiles()?.any { !it.name.startsWith(".") && it.name != "attachments" } == true
        if (!hasUserFiles && !welcomeFile.exists()) {
            welcomeFile.writeText(
                """# Welcome to Nool
A fast, lightweight, offline-first, Git-synced, linked-knowledge note-taking app.

## Features
- **Plain Markdown**: Plain files are the source of truth. Open this vault directly in Obsidian or VS Code.
- **Nested Folders**: Organize your knowledge naturally.
- **Wiki-links**: Connect thoughts with `[[Note title]]`.
- **Offline & Private**: Stored locally on your device.
- **Git Ready**: Native version control and synchronization.

Enjoy writing!
""".trimIndent()
            )
        }
    }

    private fun resolveSafe(relativePath: String): File {
        val cleanPath = relativePath.trim().removePrefix("/").removePrefix("\\")
        val file = if (cleanPath.isEmpty()) vaultRoot else File(vaultRoot, cleanPath)
        val canonicalRoot = vaultRoot.canonicalPath
        val canonicalFile = file.canonicalPath
        if (!canonicalFile.startsWith(canonicalRoot)) {
            throw SecurityException("Path escapes vault root: $relativePath")
        }
        return file
    }

    suspend fun listItems(folderRelativePath: String): List<VaultItem> = withContext(Dispatchers.IO) {
        ensureVaultInitialized()
        val folder = resolveSafe(folderRelativePath)
        if (!folder.exists() || !folder.isDirectory) {
            return@withContext emptyList()
        }

        val entries = folder.listFiles() ?: return@withContext emptyList()
        val items = mutableListOf<VaultItem>()

        for (entry in entries) {
            // Ignore hidden files / directories (e.g., .git, .app, .trash) and attachments folder
            if (entry.name.startsWith(".") || entry.name == "attachments") continue

            val relative = entry.relativeTo(vaultRoot).path.replace('\\', '/')

            if (entry.isDirectory) {
                val childCount = entry.listFiles()?.count { !it.name.startsWith(".") } ?: 0
                items.add(
                    VaultItem.FolderItem(
                        name = entry.name,
                        relativePath = relative,
                        childCount = childCount
                    )
                )
            } else if (entry.isFile && entry.name.endsWith(".md", ignoreCase = true)) {
                val title = entry.nameWithoutExtension
                val content = try { entry.readText() } catch (e: Exception) { "" }
                val parsed = com.nool.core.markdown.FrontmatterAndTagParser.parse(content)
                val excerpt = readFirstLines(entry, maxChars = 140)
                items.add(
                    VaultItem.NoteItem(
                        name = entry.name,
                        relativePath = relative,
                        title = title,
                        lastModified = entry.lastModified(),
                        size = entry.length(),
                        excerpt = excerpt,
                        isPinned = parsed.isPinned,
                        isFavorite = parsed.isFavorite,
                        isArchived = parsed.isArchived,
                        tags = parsed.tags.toList(),
                        noteType = parsed.noteType,
                        createdTimestamp = entry.lastModified()
                    )
                )
            }
        }

        // Sort: folders first (alphabetical), then notes (alphabetical by title)
        items.sortWith(
            compareBy<VaultItem> { !it.isDirectory }
                .thenBy { it.name.lowercase() }
        )

        items
    }

    private fun readFirstLines(file: File, maxChars: Int): String {
        return try {
            file.bufferedReader().use { reader ->
                val sb = StringBuilder()
                var inFrontmatter = false
                var lineCount = 0
                while (true) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    if (lineCount == 0 && trimmed == "---") {
                        inFrontmatter = true
                        lineCount++
                        continue
                    }
                    if (inFrontmatter) {
                        if (trimmed == "---") {
                            inFrontmatter = false
                        }
                        lineCount++
                        continue
                    }
                    if (trimmed.isEmpty()) continue

                    // Clean markdown headers
                    val cleanLine = trimmed.removePrefix("#").trimStart()
                    if (cleanLine.isNotEmpty()) {
                        if (sb.isNotEmpty()) sb.append(" ")
                        sb.append(cleanLine)
                    }
                    if (sb.length >= maxChars) break
                    lineCount++
                }
                if (sb.length > maxChars) sb.substring(0, maxChars) + "..." else sb.toString()
            }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun createNote(folderRelativePath: String, rawTitle: String, content: String): VaultItem.NoteItem =
        withContext(Dispatchers.IO) {
            val sanitized = sanitizeFileName(rawTitle)
            val fileName = if (sanitized.endsWith(".md", ignoreCase = true)) sanitized else "$sanitized.md"
            val parentFolder = resolveSafe(folderRelativePath)
            if (!parentFolder.exists()) {
                parentFolder.mkdirs()
            }

            var targetFile = File(parentFolder, fileName)
            var counter = 1
            val baseName = fileName.removeSuffix(".md")
            while (targetFile.exists()) {
                targetFile = File(parentFolder, "$baseName ($counter).md")
                counter++
            }

            val finalContent = if (content.isEmpty()) "# ${targetFile.nameWithoutExtension}\n\n" else content
            atomicWrite(targetFile, finalContent)

            val relative = targetFile.relativeTo(vaultRoot).path.replace('\\', '/')
            VaultItem.NoteItem(
                name = targetFile.name,
                relativePath = relative,
                title = targetFile.nameWithoutExtension,
                lastModified = targetFile.lastModified(),
                size = targetFile.length(),
                excerpt = readFirstLines(targetFile, 140)
            )
        }

    suspend fun createFolder(parentRelativePath: String, rawName: String): VaultItem.FolderItem =
        withContext(Dispatchers.IO) {
            val sanitized = sanitizeFileName(rawName)
            val parentFolder = resolveSafe(parentRelativePath)
            var targetDir = File(parentFolder, sanitized)
            var counter = 1
            while (targetDir.exists()) {
                targetDir = File(parentFolder, "$sanitized ($counter)")
                counter++
            }

            if (!targetDir.mkdirs()) {
                throw IOException("Failed to create folder: ${targetDir.absolutePath}")
            }

            val relative = targetDir.relativeTo(vaultRoot).path.replace('\\', '/')
            VaultItem.FolderItem(
                name = targetDir.name,
                relativePath = relative,
                childCount = 0
            )
        }

    suspend fun readNoteContent(relativePath: String): String = withContext(Dispatchers.IO) {
        val file = resolveSafe(relativePath)
        if (!file.exists() || !file.isFile) {
            throw IOException("File not found: $relativePath")
        }
        file.readText()
    }

    suspend fun saveNoteContent(relativePath: String, content: String): Unit = withContext(Dispatchers.IO) {
        val file = resolveSafe(relativePath)
        if (!file.parentFile!!.exists()) {
            file.parentFile!!.mkdirs()
        }
        atomicWrite(file, content)
    }

    suspend fun renameItem(relativePath: String, newName: String): String = withContext(Dispatchers.IO) {
        val source = resolveSafe(relativePath)
        if (!source.exists()) {
            throw IOException("Source does not exist: $relativePath")
        }

        val parent = source.parentFile ?: vaultRoot
        val sanitized = sanitizeFileName(newName)
        val finalTargetName = if (source.isFile && !sanitized.endsWith(".md", ignoreCase = true)) {
            "$sanitized.md"
        } else {
            sanitized
        }

        val destination = File(parent, finalTargetName)
        if (destination.exists()) {
            throw IOException("An item named '$finalTargetName' already exists in this folder.")
        }

        if (!source.renameTo(destination)) {
            throw IOException("Failed to rename '${source.name}' to '$finalTargetName'")
        }

        destination.relativeTo(vaultRoot).path.replace('\\', '/')
    }

    suspend fun deleteItem(relativePath: String): Unit = withContext(Dispatchers.IO) {
        val file = resolveSafe(relativePath)
        if (!file.exists()) return@withContext

        val success = if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
        if (!success) {
            throw IOException("Failed to delete: $relativePath")
        }
    }

    suspend fun moveItem(sourceRelativePath: String, targetFolderRelativePath: String): String =
        withContext(Dispatchers.IO) {
            val source = resolveSafe(sourceRelativePath)
            if (!source.exists()) {
                throw IOException("Source does not exist: $sourceRelativePath")
            }
            val targetDir = resolveSafe(targetFolderRelativePath)
            if (!targetDir.exists() || !targetDir.isDirectory) {
                throw IOException("Target folder does not exist: $targetFolderRelativePath")
            }

            // Prevent moving folder into itself or its child
            if (source.isDirectory) {
                val sourceCanonical = source.canonicalPath
                val targetCanonical = targetDir.canonicalPath
                if (targetCanonical == sourceCanonical || targetCanonical.startsWith(sourceCanonical + File.separator)) {
                    throw IllegalArgumentException("Cannot move a folder into itself or its subfolder.")
                }
            }

            val destination = File(targetDir, source.name)
            if (destination.exists()) {
                throw IOException("An item named '${source.name}' already exists in destination folder.")
            }

            if (!source.renameTo(destination)) {
                throw IOException("Failed to move '${source.name}' to '${targetDir.name}'")
            }

            destination.relativeTo(vaultRoot).path.replace('\\', '/')
        }

    suspend fun getAllFolders(): List<String> = withContext(Dispatchers.IO) {
        val result = mutableListOf<String>()
        result.add("") // Vault Root

        fun traverse(dir: File) {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.isDirectory && !child.name.startsWith(".")) {
                    val rel = child.relativeTo(vaultRoot).path.replace('\\', '/')
                    result.add(rel)
                    traverse(child)
                }
            }
        }

        traverse(vaultRoot)
        result.sorted()
    }

    private fun atomicWrite(file: File, content: String) {
        val parent = file.parentFile ?: vaultRoot
        val tempFile = File(parent, ".${file.name}.tmp")
        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            if (file.exists()) {
                file.delete()
            }
            if (!tempFile.renameTo(file)) {
                throw IOException("Failed to replace file during atomic write: ${file.name}")
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    fun sanitizeFileName(name: String): String {
        val trimmed = name.trim()
        val sanitized = trimmed.replace(Regex("[/\\\\:*?\"<>|]"), "_")
        return if (sanitized.isBlank()) "Untitled" else sanitized
    }

    // --- Trash Management ---

    private fun readTrashManifest(): MutableList<TrashedItem> {
        if (!trashManifestFile.exists()) return mutableListOf()
        return try {
            val jsonText = trashManifestFile.readText()
            val array = JSONArray(jsonText)
            val list = mutableListOf<TrashedItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TrashedItem(
                        id = obj.getString("id"),
                        originalRelativePath = obj.getString("originalRelativePath"),
                        name = obj.getString("name"),
                        deletedTimestamp = obj.getLong("deletedTimestamp"),
                        isDirectory = obj.optBoolean("isDirectory", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun writeTrashManifest(items: List<TrashedItem>) {
        if (!trashDir.exists()) trashDir.mkdirs()
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("originalRelativePath", item.originalRelativePath)
                put("name", item.name)
                put("deletedTimestamp", item.deletedTimestamp)
                put("isDirectory", item.isDirectory)
            }
            array.put(obj)
        }
        atomicWrite(trashManifestFile, array.toString(2))
    }

    suspend fun moveToTrash(relativePath: String): TrashedItem = withContext(Dispatchers.IO) {
        val source = resolveSafe(relativePath)
        if (!source.exists()) {
            throw IOException("File to trash not found: $relativePath")
        }

        if (!trashDir.exists()) trashDir.mkdirs()
        val id = UUID.randomUUID().toString()
        val trashTarget = File(trashDir, "$id.trash")

        if (!source.renameTo(trashTarget)) {
            throw IOException("Failed to move file to trash: $relativePath")
        }

        val trashedItem = TrashedItem(
            id = id,
            originalRelativePath = relativePath.replace('\\', '/'),
            name = source.name,
            deletedTimestamp = System.currentTimeMillis(),
            isDirectory = trashTarget.isDirectory
        )

        val manifest = readTrashManifest()
        manifest.add(0, trashedItem)
        writeTrashManifest(manifest)

        trashedItem
    }

    suspend fun listTrash(): List<TrashedItem> = withContext(Dispatchers.IO) {
        readTrashManifest()
    }

    suspend fun restoreFromTrash(trashedId: String): String = withContext(Dispatchers.IO) {
        val manifest = readTrashManifest()
        val item = manifest.find { it.id == trashedId }
            ?: throw IOException("Trashed item not found: $trashedId")

        val trashFile = File(trashDir, "$trashedId.trash")
        if (!trashFile.exists()) {
            manifest.removeAll { it.id == trashedId }
            writeTrashManifest(manifest)
            throw IOException("Trashed file is missing on disk.")
        }

        var destination = resolveSafe(item.originalRelativePath)
        val parent = destination.parentFile ?: vaultRoot
        if (!parent.exists()) {
            parent.mkdirs()
        }

        // Avoid overwrite if a file with same name was recreated
        if (destination.exists()) {
            val nameWithoutExt = destination.nameWithoutExtension
            val ext = if (destination.name.contains(".")) ".${destination.extension}" else ""
            var count = 1
            while (destination.exists()) {
                destination = File(parent, "$nameWithoutExt (Restored $count)$ext")
                count++
            }
        }

        if (!trashFile.renameTo(destination)) {
            throw IOException("Failed to restore file to: ${destination.name}")
        }

        manifest.removeAll { it.id == trashedId }
        writeTrashManifest(manifest)

        destination.relativeTo(vaultRoot).path.replace('\\', '/')
    }

    suspend fun permanentlyDeleteFromTrash(trashedId: String): Unit = withContext(Dispatchers.IO) {
        val trashFile = File(trashDir, "$trashedId.trash")
        if (trashFile.exists()) {
            if (trashFile.isDirectory) {
                trashFile.deleteRecursively()
            } else {
                trashFile.delete()
            }
        }
        val manifest = readTrashManifest()
        manifest.removeAll { it.id == trashedId }
        writeTrashManifest(manifest)
    }

    suspend fun emptyTrash(): Unit = withContext(Dispatchers.IO) {
        val entries = trashDir.listFiles() ?: return@withContext
        for (entry in entries) {
            if (entry.isDirectory) {
                entry.deleteRecursively()
            } else {
                entry.delete()
            }
        }
        writeTrashManifest(emptyList())
    }

    // --- Attachments ---

    suspend fun saveAttachment(fileName: String, inputStream: InputStream): String = withContext(Dispatchers.IO) {
        if (!attachmentsDir.exists()) attachmentsDir.mkdirs()
        val sanitized = sanitizeFileName(fileName)
        var target = File(attachmentsDir, sanitized)
        if (target.exists()) {
            val nameWithoutExt = sanitized.substringBeforeLast(".")
            val ext = if (sanitized.contains(".")) ".${sanitized.substringAfterLast(".")}" else ""
            val timestamp = System.currentTimeMillis()
            target = File(attachmentsDir, "${nameWithoutExt}_$timestamp$ext")
        }

        FileOutputStream(target).use { output ->
            inputStream.copyTo(output)
            output.flush()
        }

        "attachments/${target.name}"
    }

    fun getAttachmentFile(relativePath: String): File {
        return resolveSafe(relativePath)
    }
}
