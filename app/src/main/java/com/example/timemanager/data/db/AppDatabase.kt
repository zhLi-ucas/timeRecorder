package com.example.timemanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.timemanager.data.converter.Converters
import com.example.timemanager.data.dao.AppSettingDao
import com.example.timemanager.data.dao.CategoryDao
import com.example.timemanager.data.dao.ProjectDao
import com.example.timemanager.data.dao.ReviewDao
import com.example.timemanager.data.dao.TimeEntryDao
import com.example.timemanager.data.entity.AppSettingEntity
import com.example.timemanager.data.entity.CategoryEntity
import com.example.timemanager.data.entity.ProjectEntity
import com.example.timemanager.data.entity.ReviewEntity
import com.example.timemanager.data.entity.TimeEntryEntity

@Database(
    entities = [
        TimeEntryEntity::class,
        CategoryEntity::class,
        ProjectEntity::class,
        ReviewEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun projectDao(): ProjectDao
    abstract fun reviewDao(): ReviewDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timemanager.db"
                ).build().also { INSTANCE = it }
            }
    }
}
