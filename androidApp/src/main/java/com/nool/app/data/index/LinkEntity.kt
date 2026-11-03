package com.nool.app.data.index

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "links_index",
    indices = [
        Index(value = ["sourcePath"]),
        Index(value = ["targetTitle"]),
        Index(value = ["resolvedPath"])
    ]
)
data class LinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourcePath: String,
    val rawTarget: String,
    val targetTitle: String,
    val targetHeading: String? = null,
    val alias: String? = null,
    val isEmbed: Boolean = false,
    val resolvedPath: String? = null
)
