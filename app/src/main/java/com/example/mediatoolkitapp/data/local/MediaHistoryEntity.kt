package com.example.mediatoolkitapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_history")
data class MediaHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inputName: String,
    val inputPath: String,
    val outputPath: String?,
    val startSeconds: Long,
    val endSeconds: Long,
    val status: String,
    val message: String?,
    val createdAt: Long = System.currentTimeMillis()
)