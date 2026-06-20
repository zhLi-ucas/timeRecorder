package com.example.timemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.timemanager.data.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: ReviewEntity)

    @Update
    suspend fun update(review: ReviewEntity)

    @Delete
    suspend fun delete(review: ReviewEntity)

    @Query("SELECT * FROM reviews WHERE id = :id")
    suspend fun getById(id: String): ReviewEntity?

    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReviewEntity>>
}
