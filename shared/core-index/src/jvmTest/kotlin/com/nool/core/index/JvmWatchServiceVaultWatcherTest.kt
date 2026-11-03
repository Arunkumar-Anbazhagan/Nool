package com.nool.core.index

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmWatchServiceVaultWatcherTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testWatcherDetectsFileCreationAndModification() {
        val root = tempFolder.newFolder("vault")
        val watcher = JvmWatchServiceVaultWatcher()

        val changedLatch = CountDownLatch(1)
        var changedPath: String? = null

        watcher.startWatching(
            vaultPath = root.absolutePath,
            onFileChanged = { path ->
                changedPath = path
                changedLatch.countDown()
            },
            onFileDeleted = {}
        )

        val newFile = File(root, "WatcherNote.md")
        newFile.writeText("# Testing Watcher")

        val detected = changedLatch.await(5, TimeUnit.SECONDS)
        watcher.stopWatching()

        assertTrue(detected, "Expected file creation event to be detected")
        assertEquals("WatcherNote.md", changedPath)
    }
}
