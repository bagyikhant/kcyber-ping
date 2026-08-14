package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PingDao {
    @Query("SELECT * FROM ping_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllHistory(): Flow<List<PingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PingHistoryEntity): Long

    @Query("DELETE FROM ping_history")
    suspend fun clearAllHistory()

    @Query("DELETE FROM ping_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("SELECT * FROM saved_hosts ORDER BY isFavorite DESC, label ASC")
    fun getAllSavedHosts(): Flow<List<SavedHostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedHost(host: SavedHostEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavedHosts(hosts: List<SavedHostEntity>)

    @Update
    suspend fun updateSavedHost(host: SavedHostEntity)

    @Delete
    suspend fun deleteSavedHost(host: SavedHostEntity)
}

@Database(entities = [PingHistoryEntity::class, SavedHostEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pingDao(): PingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kcyber_ping_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
