package com.nool.app.data.index

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        NoteFtsEntity::class,
        LinkEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NoolDatabase : RoomDatabase() {
    abstract fun indexDao(): IndexDao

    companion object {
        const val DATABASE_NAME = "nool_index_cache.db"

        fun create(context: Context): NoolDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NoolDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigration()
             .build()
        }
    }
}
