package com.nool.app.data.index

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class NoteTitleAndPath(
    val title: String,
    val relativePath: String,
    val aliases: String = ""
)

@Dao
interface IndexDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes_index WHERE relativePath = :relativePath")
    suspend fun deleteNote(relativePath: String)

    @Query("DELETE FROM notes_index")
    suspend fun clearAll()

    @Query("SELECT * FROM notes_index WHERE relativePath = :relativePath LIMIT 1")
    suspend fun getNote(relativePath: String): NoteEntity?

    @Query("SELECT * FROM notes_index WHERE isArchived = 0 ORDER BY isPinned DESC, lastModified DESC")
    fun observeActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes_index WHERE folderPath = :folderPath AND isArchived = 0 ORDER BY isPinned DESC, lastModified DESC")
    fun observeNotesInFolder(folderPath: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes_index ORDER BY lastModified DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT tags FROM notes_index WHERE isArchived = 0")
    suspend fun getAllTagsRaw(): List<String>

    @Query("""
        SELECT notes_index.* FROM notes_index
        JOIN notes_fts ON notes_index.rowid = notes_fts.rowid
        WHERE notes_fts MATCH :query
        ORDER BY notes_index.isPinned DESC, notes_index.lastModified DESC
    """)
    suspend fun searchFts(query: String): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes_index")
    suspend fun countNotes(): Int

    // Links & Backlinks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<LinkEntity>)

    @Query("DELETE FROM links_index WHERE sourcePath = :sourcePath")
    suspend fun deleteLinksForSource(sourcePath: String)

    @Query("DELETE FROM links_index")
    suspend fun clearAllLinks()

    @Query("SELECT * FROM links_index WHERE sourcePath = :sourcePath")
    suspend fun getLinksFromSource(sourcePath: String): List<LinkEntity>

    @Query("SELECT * FROM links_index")
    suspend fun getAllLinks(): List<LinkEntity>

    @Query("""
        SELECT * FROM links_index
        WHERE resolvedPath = :targetPath
           OR (resolvedPath IS NULL AND LOWER(targetTitle) = LOWER(:targetTitle))
    """)
    suspend fun getBacklinksForNote(targetTitle: String, targetPath: String): List<LinkEntity>

    @Query("SELECT title, relativePath, aliases FROM notes_index")
    suspend fun getAllNoteTitlesAndPaths(): List<NoteTitleAndPath>

    @Query("""
        SELECT DISTINCT sourcePath FROM links_index
        WHERE LOWER(targetTitle) = LOWER(:targetTitle)
           OR resolvedPath = :targetPath
    """)
    suspend fun getReferencingSourcePaths(targetTitle: String, targetPath: String): List<String>
}
