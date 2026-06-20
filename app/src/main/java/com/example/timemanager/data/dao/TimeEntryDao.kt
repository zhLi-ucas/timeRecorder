package com.example.timemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.timemanager.data.entity.TimeEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class CategoryDurationRow(
    val categoryId: String,
    val totalMin: Long
)

@Dao
interface TimeEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TimeEntryEntity)

    @Update
    suspend fun update(entry: TimeEntryEntity)

    @Delete
    suspend fun delete(entry: TimeEntryEntity)

    @Query("SELECT * FROM time_entries WHERE date = :date ORDER BY startMinOfDay ASC")
    fun observeByDate(date: LocalDate): Flow<List<TimeEntryEntity>>

    @Query("SELECT * FROM time_entries WHERE date = :date ORDER BY startMinOfDay ASC")
    suspend fun getByDate(date: LocalDate): List<TimeEntryEntity>

    @Query("SELECT * FROM time_entries WHERE date BETWEEN :from AND :to ORDER BY date, startMinOfDay")
    suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<TimeEntryEntity>

    @Query("SELECT COALESCE(MAX(startMinOfDay + durationMin), 0) FROM time_entries WHERE date = :date")
    suspend fun getMaxEndMinOfDay(date: LocalDate): Int

    @Query(
        """
        SELECT categoryId, SUM(durationMin) AS totalMin
        FROM time_entries
        WHERE date BETWEEN :from AND :to
        GROUP BY categoryId
        """
    )
    suspend fun sumDurationByCategory(from: LocalDate, to: LocalDate): List<CategoryDurationRow>
}
