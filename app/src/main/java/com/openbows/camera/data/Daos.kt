package com.openbows.camera.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM watermark_templates ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WatermarkTemplate>>

    @Query("SELECT * FROM watermark_templates ORDER BY id ASC")
    suspend fun getAll(): List<WatermarkTemplate>

    @Query("SELECT * FROM watermark_templates WHERE id = :id")
    suspend fun getById(id: Long): WatermarkTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: WatermarkTemplate): Long

    @Update
    suspend fun update(template: WatermarkTemplate)

    @Delete
    suspend fun delete(template: WatermarkTemplate)

    @Query("SELECT COUNT(*) FROM watermark_templates")
    suspend fun count(): Int
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY takenAt DESC")
    fun observeAll(): Flow<List<PhotoRecord>>

    @Query("SELECT * FROM photos ORDER BY takenAt DESC LIMIT 1")
    fun observeLatest(): Flow<PhotoRecord?>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: Long): PhotoRecord?

    @Insert
    suspend fun insert(photo: PhotoRecord): Long

    @Delete
    suspend fun delete(photo: PhotoRecord)
}

@Dao
interface RemarkHistoryDao {
    @Query("SELECT * FROM remark_history ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<RemarkHistory>>

    @Query("SELECT * FROM remark_history WHERE text = :text LIMIT 1")
    suspend fun findByText(text: String): RemarkHistory?

    @Insert
    suspend fun insert(item: RemarkHistory): Long

    @Update
    suspend fun update(item: RemarkHistory)

    @Delete
    suspend fun delete(item: RemarkHistory)
}
