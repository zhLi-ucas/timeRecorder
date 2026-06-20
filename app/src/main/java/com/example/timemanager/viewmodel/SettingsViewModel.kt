package com.example.timemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.DefaultDataSeeder
import com.example.timemanager.data.db.AppDatabase
import com.example.timemanager.data.entity.AppSettingEntity
import com.example.timemanager.data.entity.CategoryEntity
import com.example.timemanager.data.entity.ProjectEntity
import com.example.timemanager.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val categoryDao = db.categoryDao()
    private val projectDao = db.projectDao()
    private val entryDao = db.timeEntryDao()
    private val reviewDao = db.reviewDao()
    private val settingDao = db.appSettingDao()

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = projectDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dayStartMin = MutableStateFlow(480)
    val dayStartMin: StateFlow<Int> = _dayStartMin.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        viewModelScope.launch {
            _dayStartMin.value =
                settingDao.getByKey(DefaultDataSeeder.KEY_DAY_START_MIN)?.toIntOrNull() ?: 480
        }
    }

    fun consumeToast() { _toast.value = null }

    // --- Category CRUD ---

    fun addFirstLevel(name: String, colorKey: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val maxOrder = (categories.value.filter { it.parentId == null }.maxOfOrNull { it.sortOrder } ?: -1) + 1
            categoryDao.insert(
                CategoryEntity(
                    id = "cat_custom_${UUID.randomUUID().toString().take(8)}",
                    name = name.trim(),
                    parentId = null,
                    colorKey = colorKey,
                    sortOrder = maxOrder,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun addSecondLevel(parentId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val maxOrder = (categories.value.filter { it.parentId == parentId }.maxOfOrNull { it.sortOrder } ?: -1) + 1
            categoryDao.insert(
                CategoryEntity(
                    id = "cat_custom_${UUID.randomUUID().toString().take(8)}",
                    name = name.trim(),
                    parentId = parentId,
                    colorKey = null,
                    sortOrder = maxOrder,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun renameCategory(id: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val cat = categoryDao.getById(id) ?: return@launch
            categoryDao.update(cat.copy(name = newName.trim(), updatedAt = LocalDateTime.now()))
        }
    }

    fun setCategoryColor(id: String, colorKey: String) {
        viewModelScope.launch {
            val cat = categoryDao.getById(id) ?: return@launch
            categoryDao.update(cat.copy(colorKey = colorKey, updatedAt = LocalDateTime.now()))
        }
    }

    fun archiveCategory(id: String, archived: Boolean) {
        viewModelScope.launch {
            val cat = categoryDao.getById(id) ?: return@launch
            if (cat.isSystem) return@launch
            categoryDao.update(cat.copy(isArchived = archived, updatedAt = LocalDateTime.now()))
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            val cat = categoryDao.getById(id) ?: return@launch
            if (cat.isSystem) {
                _toast.value = "系统分类不可删除"
                return@launch
            }
            categoryDao.delete(cat)
        }
    }

    // --- Project CRUD ---

    fun addProject(name: String, description: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val now = LocalDateTime.now()
            projectDao.insert(
                ProjectEntity(
                    id = "proj_${UUID.randomUUID().toString().take(8)}",
                    name = name.trim(),
                    description = description?.ifBlank { null },
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun renameProject(id: String, newName: String, newDescription: String?) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val p = projectDao.getById(id) ?: return@launch
            projectDao.update(
                p.copy(
                    name = newName.trim(),
                    description = newDescription?.ifBlank { null },
                    updatedAt = LocalDateTime.now()
                )
            )
        }
    }

    fun archiveProject(id: String, archived: Boolean) {
        viewModelScope.launch {
            val p = projectDao.getById(id) ?: return@launch
            projectDao.update(p.copy(isArchived = archived, updatedAt = LocalDateTime.now()))
        }
    }

    // --- Day start ---

    fun setDayStartMin(min: Int) {
        val clamped = min.coerceIn(0, 1439)
        _dayStartMin.value = clamped
        viewModelScope.launch {
            settingDao.upsert(AppSettingEntity(DefaultDataSeeder.KEY_DAY_START_MIN, clamped.toString()))
        }
    }

    // --- Export ---

    suspend fun buildCsv(): String {
        val entries = entryDao.getByDateRange(
            LocalDate.MIN,
            LocalDate.of(9999, 12, 31)
        )
        val cats = categoryDao.getAll()
        val catById = cats.associateBy { it.id }
        val projects = projectDao.getActive()
        val projById = projects.associateBy { it.id }
        return CsvExporter.toCsv(entries, catById, projById)
    }

    suspend fun buildMarkdown(): String {
        val entries = entryDao.getByDateRange(LocalDate.MIN, LocalDate.of(9999, 12, 31))
        val cats = categoryDao.getAll()
        val catById = cats.associateBy { it.id }
        val projects = projectDao.getActive()
        val projById = projects.associateBy { it.id }
        return CsvExporter.toMarkdown(entries, catById, projById)
    }

    suspend fun buildJsonBackup(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        val cats = JSONArray()
        categoryDao.getAll().forEach { c ->
            val o = JSONObject()
            o.put("id", c.id); o.put("name", c.name)
            o.put("parentId", c.parentId ?: JSONObject.NULL)
            o.put("colorKey", c.colorKey ?: JSONObject.NULL)
            o.put("sortOrder", c.sortOrder)
            o.put("isArchived", c.isArchived)
            o.put("isSystem", c.isSystem)
            cats.put(o)
        }
        root.put("categories", cats)

        val projs = JSONArray()
        projectDao.getActive().forEach { p ->
            val o = JSONObject()
            o.put("id", p.id); o.put("name", p.name)
            o.put("description", p.description ?: JSONObject.NULL)
            o.put("isArchived", p.isArchived)
            projs.put(o)
        }
        root.put("projects", projs)

        val entries = JSONArray()
        entryDao.getByDateRange(LocalDate.MIN, LocalDate.of(9999, 12, 31)).forEach { e ->
            val o = JSONObject()
            o.put("id", e.id)
            o.put("date", e.date.toString())
            o.put("startMinOfDay", e.startMinOfDay)
            o.put("durationMin", e.durationMin)
            o.put("title", e.title)
            o.put("categoryId", e.categoryId)
            o.put("projectId", e.projectId ?: JSONObject.NULL)
            o.put("note", e.note ?: JSONObject.NULL)
            o.put("isEstimated", e.isEstimated)
            entries.put(o)
        }
        root.put("timeEntries", entries)

        val reviews = JSONArray()
        reviewDao.observeAll().first().forEach { r ->
            val o = JSONObject()
            o.put("id", r.id)
            o.put("periodType", r.periodType)
            o.put("periodStart", r.periodStart.toString())
            o.put("periodEnd", r.periodEnd.toString())
            o.put("summaryText", r.summaryText ?: JSONObject.NULL)
            o.put("mainFindings", r.mainFindings ?: JSONObject.NULL)
            o.put("adjustmentPlan", r.adjustmentPlan ?: JSONObject.NULL)
            reviews.put(o)
        }
        root.put("reviews", reviews)

        val settings = JSONArray()
        settingDao.getAll().forEach { s ->
            val o = JSONObject()
            o.put("key", s.key); o.put("value", s.value)
            settings.put(o)
        }
        root.put("settings", settings)

        return root.toString(2)
    }

    suspend fun restoreFromJson(json: String): Int {
        val root = JSONObject(json)
        var count = 0
        val now = LocalDateTime.now()

        root.optJSONArray("categories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                categoryDao.insert(
                    CategoryEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        parentId = if (o.isNull("parentId")) null else o.getString("parentId"),
                        colorKey = if (o.isNull("colorKey")) null else o.getString("colorKey"),
                        sortOrder = o.optInt("sortOrder", 0),
                        isArchived = o.optBoolean("isArchived", false),
                        isSystem = o.optBoolean("isSystem", false),
                        createdAt = now,
                        updatedAt = now
                    )
                )
                count++
            }
        }

        root.optJSONArray("projects")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                projectDao.insert(
                    ProjectEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        description = if (o.isNull("description")) null else o.getString("description"),
                        isArchived = o.optBoolean("isArchived", false),
                        createdAt = now,
                        updatedAt = now
                    )
                )
                count++
            }
        }

        root.optJSONArray("timeEntries")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                entryDao.insert(
                    com.example.timemanager.data.entity.TimeEntryEntity(
                        id = o.getString("id"),
                        date = java.time.LocalDate.parse(o.getString("date")),
                        startMinOfDay = o.getInt("startMinOfDay"),
                        durationMin = o.getInt("durationMin"),
                        title = o.getString("title"),
                        categoryId = o.getString("categoryId"),
                        projectId = if (o.isNull("projectId")) null else o.getString("projectId"),
                        note = if (o.isNull("note")) null else o.getString("note"),
                        isEstimated = o.optBoolean("isEstimated", false),
                        createdAt = now,
                        updatedAt = now
                    )
                )
                count++
            }
        }

        root.optJSONArray("reviews")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                reviewDao.insert(
                    com.example.timemanager.data.entity.ReviewEntity(
                        id = o.getString("id"),
                        periodType = o.getString("periodType"),
                        periodStart = java.time.LocalDate.parse(o.getString("periodStart")),
                        periodEnd = java.time.LocalDate.parse(o.getString("periodEnd")),
                        summaryText = if (o.isNull("summaryText")) null else o.getString("summaryText"),
                        mainFindings = if (o.isNull("mainFindings")) null else o.getString("mainFindings"),
                        adjustmentPlan = if (o.isNull("adjustmentPlan")) null else o.getString("adjustmentPlan"),
                        createdAt = now,
                        updatedAt = now
                    )
                )
                count++
            }
        }

        root.optJSONArray("settings")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                settingDao.upsert(AppSettingEntity(o.getString("key"), o.getString("value")))
                count++
            }
        }

        return count
    }
}
