package com.nool.app.data.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.nool.app.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SharedContent(
    val text: String? = null,
    val subject: String? = null,
    val uris: List<Uri> = emptyList()
)

sealed interface ShareTarget {
    data class NewNote(val title: String, val folder: String) : ShareTarget
    data class AppendToNote(val noteRelativePath: String) : ShareTarget
}

class ShareManager(
    private val context: Context? = null,
    private val vaultRepository: VaultRepository
) {
    fun extractSharedContent(intent: Intent): SharedContent? {
        val action = intent.action ?: return null
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            return null
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val uris = mutableListOf<Uri>()

        if (action == Intent.ACTION_SEND) {
            val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (streamUri != null) {
                uris.add(streamUri)
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            val streamUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            if (streamUris != null) {
                uris.addAll(streamUris.filterNotNull())
            }
        }

        if (text.isNullOrBlank() && uris.isEmpty()) {
            return null
        }

        return SharedContent(text = text, subject = subject, uris = uris)
    }

    suspend fun saveSharedContent(
        content: SharedContent,
        target: ShareTarget
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val savedAttachments = mutableListOf<String>()

            for (uri in content.uris) {
                val fileName = getFileNameFromUri(uri)
                val resolver = context?.contentResolver ?: throw java.io.IOException("Context is required to resolve stream URIs")
                val inputStream: InputStream = resolver.openInputStream(uri)
                    ?: throw java.io.IOException("Unable to open input stream for $uri")
                val path = inputStream.use { stream ->
                    vaultRepository.saveAttachment(fileName, stream).getOrThrow()
                }
                savedAttachments.add(path)
            }

            val body = formatMarkdown(content.text, savedAttachments)

            when (target) {
                is ShareTarget.NewNote -> {
                    val defaultTitle = content.subject?.takeIf { it.isNotBlank() }
                        ?: "Shared ${SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.getDefault()).format(Date())}"
                    val finalTitle = target.title.ifBlank { defaultTitle }
                    val note = vaultRepository.createNote(target.folder, finalTitle, body).getOrThrow()
                    note.relativePath
                }
                is ShareTarget.AppendToNote -> {
                    val existing = vaultRepository.readNoteContent(target.noteRelativePath).getOrThrow()
                    val newContent = if (existing.isBlank()) body else "$existing\n\n$body"
                    vaultRepository.saveNoteContent(target.noteRelativePath, newContent).getOrThrow()
                    target.noteRelativePath
                }
            }
        }
    }

    fun formatMarkdown(text: String?, attachments: List<String>): String {
        val sb = StringBuilder()
        if (!text.isNullOrBlank()) {
            sb.append(text.trim())
        }
        for (att in attachments) {
            if (sb.isNotEmpty()) {
                sb.append("\n\n")
            }
            sb.append("![[$att]]")
        }
        return sb.toString()
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "image_${System.currentTimeMillis()}.jpg"
        if (uri.scheme == "content") {
            try {
                context?.contentResolver?.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        cursor.getString(nameIndex)?.let { name = it }
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        } else if (uri.scheme == "file") {
            uri.lastPathSegment?.let { name = it }
        }
        return name
    }
}
