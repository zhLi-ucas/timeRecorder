package com.example.timemanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timemanager.data.DefaultDataSeeder
import com.example.timemanager.data.entity.CategoryEntity
import com.example.timemanager.data.entity.TimeEntryEntity
import com.example.timemanager.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class TimeEntryDaoTest {
    private lateinit var db: AppDatabase

    private val today = LocalDate.of(2026, 6, 20)
    private val now = LocalDateTime.of(2026, 6, 20, 10, 0)

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_getByDate_returnsEntry() = runBlocking {
        db.timeEntryDao().insert(sampleEntry("e1", 480, 60))

        val result = db.timeEntryDao().getByDate(today)
        assertEquals(1, result.size)
        assertEquals("e1", result[0].id)
    }

    @Test
    fun getByDate_sortedByStartAsc() = runBlocking {
        db.timeEntryDao().insert(sampleEntry("late", 720, 30))
        db.timeEntryDao().insert(sampleEntry("early", 480, 60))

        val result = db.timeEntryDao().getByDate(today)
        assertEquals(listOf("early", "late"), result.map { it.id })
    }

    @Test
    fun getMaxEndMinOfDay_zeroWhenEmpty() = runBlocking {
        assertEquals(0, db.timeEntryDao().getMaxEndMinOfDay(today))
    }

    @Test
    fun getMaxEndMinOfDay_returnsLatestEnd() = runBlocking {
        db.timeEntryDao().insert(sampleEntry("a", 480, 60))   // ends 540
        db.timeEntryDao().insert(sampleEntry("b", 700, 90))   // ends 790

        assertEquals(790, db.timeEntryDao().getMaxEndMinOfDay(today))
    }

    @Test
    fun sumDurationByCategory_groupsAndSums() = runBlocking {
        seedCategory("cat_a")
        seedCategory("cat_b")
        db.timeEntryDao().insert(sampleEntry("e1", 480, 60, categoryId = "cat_a"))
        db.timeEntryDao().insert(sampleEntry("e2", 600, 30, categoryId = "cat_a"))
        db.timeEntryDao().insert(sampleEntry("e3", 700, 90, categoryId = "cat_b"))

        val rows = db.timeEntryDao().sumDurationByCategory(today, today)
        assertEquals(2, rows.size)
        val a = rows.first { it.categoryId == "cat_a" }
        val b = rows.first { it.categoryId == "cat_b" }
        assertEquals(90L, a.totalMin)
        assertEquals(90L, b.totalMin)
    }

    @Test
    fun seeder_populatesFirstAndSecondLevel() = runBlocking {
        DefaultDataSeeder(db).seedIfNeeded()

        val first = db.categoryDao().getFirstLevel()
        assertEquals(8, first.size)
        assertEquals("核心工作", first[0].name)
        assertEquals("blue", first[0].colorKey)
        assertEquals("未分类", first[7].name)
        assertTrue(first[7].isSystem)

        val secondOfCore = db.categoryDao().getSecondLevel("cat_core_work")
        assertEquals(5, secondOfCore.size)
        assertEquals("写作", secondOfCore[0].name)

        assertEquals("true", db.appSettingDao().getByKey("seeded"))
        assertEquals("480", db.appSettingDao().getByKey("day_start_min"))
    }

    @Test
    fun seeder_isIdempotent() = runBlocking {
        DefaultDataSeeder(db).seedIfNeeded()
        DefaultDataSeeder(db).seedIfNeeded()

        assertEquals(8, db.categoryDao().getFirstLevel().size)
    }

    private fun sampleEntry(
        id: String,
        startMinOfDay: Int,
        durationMin: Int,
        categoryId: String = "cat_x"
    ) = TimeEntryEntity(
        id = id,
        date = today,
        startMinOfDay = startMinOfDay,
        durationMin = durationMin,
        title = "test",
        categoryId = categoryId,
        createdAt = now,
        updatedAt = now
    )

    private suspend fun seedCategory(id: String) {
        db.categoryDao().insert(
            CategoryEntity(
                id = id,
                name = id,
                parentId = null,
                colorKey = null,
                sortOrder = 0,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
