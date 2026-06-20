package com.example.timemanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timemanager.data.entity.AppSettingEntity

@Dao
interface AppSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: AppSettingEntity)

    @Query("SELECT value FROM app_settings WHERE key = :key")
    suspend fun getByKey(key: String): String?

    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettingEntity>
}
