package com.nool.app.data.index

import android.os.Build
import android.os.FileObserver
import com.nool.core.index.VaultWatcher
import java.io.File

class AndroidFileObserverVaultWatcher : VaultWatcher {
    private var observer: FileObserver? = null

    override fun startWatching(
        vaultPath: String,
        onFileChanged: (relativePath: String) -> Unit,
        onFileDeleted: (relativePath: String) -> Unit
    ) {
        stopWatching()
        val vaultDir = File(vaultPath)
        if (!vaultDir.exists()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                observer = object : FileObserver(
                    vaultDir,
                    MODIFY or CREATE or DELETE or MOVED_TO or MOVED_FROM
                ) {
                    override fun onEvent(event: Int, path: String?) {
                        if (path == null) return
                        if (path.startsWith(".git") || path.startsWith(".nool_cache") || path.startsWith("attachments/")) return
                        if (!path.endsWith(".md", ignoreCase = true)) return

                        when (event and ALL_EVENTS) {
                            MODIFY, CREATE, MOVED_TO -> onFileChanged(path)
                            DELETE, MOVED_FROM -> onFileDeleted(path)
                        }
                    }
                }
                observer?.startWatching()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stopWatching() {
        observer?.stopWatching()
        observer = null
    }
}
