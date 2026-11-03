package com.nool.app

import android.content.Context
import android.content.ContextWrapper
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nool.app.data.index.IndexMigrationHelper
import com.nool.core.index.SqlDelightIndexManager
import com.nool.core.index.db.NoolDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class IndexMigrationHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var vaultDir: File
    private lateinit var dbDir: File
    private lateinit var sqlDelightManager: SqlDelightIndexManager

    private class TestContext(private val baseDbDir: File) : ContextWrapper(null) {
        override fun getDatabasePath(name: String): File {
            return File(baseDbDir, name)
        }

        override fun deleteDatabase(name: String): Boolean {
            return File(baseDbDir, name).delete()
        }
    }

    @Before
    fun setup() {
        vaultDir = tempFolder.newFolder("migration_vault")
        dbDir = tempFolder.newFolder("databases")

        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NoolDatabase.Schema.create(driver)
        val db = NoolDatabase(driver)
        sqlDelightManager = SqlDelightIndexManager(db)
    }

    @After
    fun tearDown() {
        vaultDir.deleteRecursively()
        dbDir.deleteRecursively()
    }

    @Test
    fun testNoMigrationWhenNoOldRoomDb() = runTest {
        val testContext = TestContext(dbDir)
        val migrated = IndexMigrationHelper.performMigrationIfNeeded(
            context = testContext,
            vaultRoot = vaultDir,
            sqlDelightManager = sqlDelightManager
        )
        assertFalse(migrated)
    }

    @Test
    fun testSuccessfulMigrationDeletesOldRoomDb() = runTest {
        val testContext = TestContext(dbDir)
        val oldDb = File(dbDir, "nool_index_cache.db")
        oldDb.writeText("fake room database content")
        assertTrue(oldDb.exists())

        // Create sample markdown note in vault
        val noteFile = File(vaultDir, "Welcome.md")
        noteFile.writeText("# Welcome\nHello from Nool!")

        val migrated = IndexMigrationHelper.performMigrationIfNeeded(
            context = testContext,
            vaultRoot = vaultDir,
            sqlDelightManager = sqlDelightManager
        )

        assertTrue(migrated)
        assertFalse("Old Room DB file should be deleted after successful migration", oldDb.exists())

        val indexedNote = sqlDelightManager.getNote("Welcome.md")
        assertNotNull(indexedNote)
        assertEquals("Welcome", indexedNote!!.title)
    }
}
