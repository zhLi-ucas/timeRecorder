package com.example.timemanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
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
                )
                    .addMigrations(MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE time_entries ADD COLUMN effectiveness INTEGER NOT NULL DEFAULT 80"
                )
                db.execSQL(
                    """UPDATE time_entries SET effectiveness = 20
                       WHERE categoryId IN (SELECT id FROM categories WHERE parentId = 'cat_invalid')"""
                )
            }
        }
    }
}
