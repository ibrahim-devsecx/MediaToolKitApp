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

    @Query(
        """
        SELECT * FROM media_history 
        WHERE status = 'SUCCESS' 
        AND outputPath IS NOT NULL 
        ORDER BY createdAt DESC
    """
    )
    fun observeProcessedFiles(): Flow<List<MediaHistoryEntity>>

    @Query("DELETE FROM media_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM media_history")
    suspend fun clearHistory()
}