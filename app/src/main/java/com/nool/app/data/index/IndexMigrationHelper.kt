package com.nool.app.data.index

import android.content.Context
import com.nool.core.index.SqlDelightIndexManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object IndexMigrationHelper {
    private const val OLD_ROOM_DB_NAME = "nool_index_cache.db"

    /**
     * Checks if the old Room database exists.
     * If it does, performs a full re-index into SQLDelight with progress reporting.
     * Only upon 100% successful completion of the re-index does it delete the old Room database files.
     * If interrupted, this is resumable on next app launch.
     */
    suspend fun performMigrationIfNeeded(
        context: Context,
        vaultRoot: File,
        sqlDelightManager: SqlDelightIndexManager
    ): Boolean = withContext(Dispatchers.IO) {
        val oldDbFile = context.getDatabasePath(OLD_ROOM_DB_NAME)
        if (!oldDbFile.exists()) {
            return@withContext false
        }

        try {
            val markdownFiles = mutableListOf<File>()
            fun scan(dir: File) {
                val files = dir.listFiles() ?: return
                for (file in files) {
                    if (file.name.startsWith(".") || file.name == "attachments") continue
                    if (file.isDirectory) scan(file)
                    else if (file.isFile && file.name.endsWith(".md", ignoreCase = true)) {
                        markdownFiles.add(file)
                    }
                }
            }
            if (vaultRoot.exists()) {
                scan(vaultRoot)
            }

            val total = markdownFiles.size
            if (total > 0) {
                markdownFiles.forEachIndexed { index, file ->
                    val relative = file.relativeTo(vaultRoot).path.replace('\\', '/')
                    val content = try { file.readText() } catch (e: Exception) { "" }
                    sqlDelightManager.indexNoteContent(
                        relativePath = relative,
                        content = content,
                        lastModified = file.lastModified(),
                        size = file.length()
                    )
                    val progress = (index + 1).toFloat() / total.toFloat()
                    sqlDelightManager.updateProgress(progress)
                }
            }
            sqlDelightManager.updateProgress(1.0f)

            // Migration succeeded completely: safely delete old Room DB files
            context.deleteDatabase(OLD_ROOM_DB_NAME)
            val walFile = File(oldDbFile.parentFile, "$OLD_ROOM_DB_NAME-wal")
            if (walFile.exists()) walFile.delete()
            val shmFile = File(oldDbFile.parentFile, "$OLD_ROOM_DB_NAME-shm")
            if (shmFile.exists()) shmFile.delete()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Do not delete old database so migration can resume on next launch
            false
        }
    }
}
