package com.example.timemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.timemanager.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY sortOrder")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE parentId IS NULL AND isArchived = 0 ORDER BY sortOrder")
    suspend fun getFirstLevel(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE parentId = :parentId AND isArchived = 0 ORDER BY sortOrder")
    suspend fun getSecondLevel(parentId: String): List<CategoryEntity>
}
