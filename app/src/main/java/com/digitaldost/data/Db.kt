package com.digitaldost.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,          // "user" or "assistant"
    val text: String,
    val ts: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM messages ORDER BY id ASC")
    fun observeAll(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM messages ORDER BY id DESC LIMIT :n")
    suspend fun recent(n: Int): List<ChatMessage>

    @Insert
    suspend fun insert(m: ChatMessage)

    @Query("DELETE FROM messages")
    suspend fun clear()
}

@Database(entities = [ChatMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "digital_dost.db")
                .build().also { instance = it }
        }
    }
}
