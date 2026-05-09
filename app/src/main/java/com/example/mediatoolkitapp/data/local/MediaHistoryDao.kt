package com.example.mediatoolkitapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: MediaHistoryEntity)

    @Query("SELECT * FROM media_history ORDER BY createdAt DESC")
    fun observeHistory(): Flow<List<MediaHistoryEntity>>

    @Query("DELETE FROM media_history")
    suspend fun clearHistory()
}