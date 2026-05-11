package com.example.mediatoolkitapp.data.repository

import com.example.mediatoolkitapp.data.local.MediaHistoryEntity
import kotlinx.coroutines.flow.Flow

interface MediaRepository {

    fun observeHistory(): Flow<List<MediaHistoryEntity>>

    fun observeProcessedFiles(): Flow<List<MediaHistoryEntity>>

    suspend fun saveHistory(history: MediaHistoryEntity)

    suspend fun deleteHistoryById(id: Long)
}