package com.example.mediatoolkitapp.data.repository

import com.example.mediatoolkitapp.data.local.MediaHistoryDao
import com.example.mediatoolkitapp.data.local.MediaHistoryEntity
import kotlinx.coroutines.flow.Flow

class MediaRepositoryImpl(
    private val mediaHistoryDao: MediaHistoryDao
) : MediaRepository {

    override fun observeHistory(): Flow<List<MediaHistoryEntity>> {
        return mediaHistoryDao.observeHistory()
    }

    override fun observeProcessedFiles(): Flow<List<MediaHistoryEntity>> {
        return mediaHistoryDao.observeProcessedFiles()
    }

    override suspend fun saveHistory(history: MediaHistoryEntity) {
        mediaHistoryDao.insertHistory(history)
    }

    override suspend fun deleteHistoryById(id: Long) {
        mediaHistoryDao.deleteHistoryById(id)
    }
}