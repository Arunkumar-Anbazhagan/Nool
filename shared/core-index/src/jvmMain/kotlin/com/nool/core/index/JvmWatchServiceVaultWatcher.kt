package com.nool.core.index

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.concurrent.thread

class JvmWatchServiceVaultWatcher : VaultWatcher {
    private var watchService: WatchService? = null
    @Volatile
    private var isRunning = false
    private var watchThread: Thread? = null

    override fun startWatching(
        vaultPath: String,
        onFileChanged: (relativePath: String) -> Unit,
        onFileDeleted: (relativePath: String) -> Unit
    ) {
        stopWatching()
        val vaultDir = File(vaultPath)
        if (!vaultDir.exists() || !vaultDir.isDirectory) return

        try {
            val ws = FileSystems.getDefault().newWatchService()
            watchService = ws
            isRunning = true

            val rootPath = Paths.get(vaultPath)
            val keyMap = mutableMapOf<WatchKey, Path>()

            vaultDir.walkTopDown()
                .filter { it.isDirectory && !it.name.startsWith(".") && it.name != "attachments" }
                .forEach { dir ->
                    val p = dir.toPath()
                    val key = p.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                    keyMap[key] = p
                }

            watchThread = thread(isDaemon = true, name = "Nool-JvmVaultWatcher") {
                while (isRunning) {
                    val key = try {
                        ws.take()
                    } catch (e: Exception) {
                        break
                    }

                    val dirPath = keyMap[key] ?: rootPath
                    for (event in key.pollEvents()) {
                        val kind = event.kind()
                        if (kind == OVERFLOW) continue

                        val filename = event.context() as? Path ?: continue
                        val fullPath = dirPath.resolve(filename)
                        val relative = rootPath.relativize(fullPath).toString().replace('\\', '/')

                        if (relative.startsWith(".git") || relative.startsWith(".nool") || relative.startsWith("attachments/")) continue
                        if (!relative.endsWith(".md", ignoreCase = true)) continue

                        when (kind) {
                            ENTRY_CREATE, ENTRY_MODIFY -> onFileChanged(relative)
                            ENTRY_DELETE -> onFileDeleted(relative)
                        }
                    }

                    val valid = key.reset()
                    if (!valid) {
                        keyMap.remove(key)
                        if (keyMap.isEmpty()) break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stopWatching() {
        isRunning = false
        try {
            watchService?.close()
        } catch (e: Exception) {
            // ignore
        }
        watchService = null
        watchThread?.interrupt()
        watchThread = null
    }
}
