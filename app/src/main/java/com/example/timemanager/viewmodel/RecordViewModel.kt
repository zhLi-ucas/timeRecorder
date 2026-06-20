package com.example.timemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.DefaultDataSeeder
import com.example.timemanager.data.db.AppDatabase
import com.example.timemanager.data.entity.ProjectEntity
import com.example.timemanager.data.entity.TimeEntryEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class RecordFormState(
    val editingId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val parentCategoryId: String? = null,
    val categoryId: String? = null,
    val projectId: String? = null,
    val durationMin: Int = 60,
    val title: String = "",
    val note: String = "",
    val originalStartMinOfDay: Int = 0,
    val originalCreatedAt: LocalDateTime? = null
) {
    val isEditing: Boolean get() = editingId != null
}

sealed interface RecordUiEvent {
    data object Saved : RecordUiEvent
    data class Toast(val msg: String) : RecordUiEvent
}

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val entryDao = db.timeEntryDao()
    private val categoryDao = db.categoryDao()
    private val projectDao = db.projectDao()
    private val settingDao = db.appSettingDao()
    private val saveMutex = Mutex()

    private val _form = MutableStateFlow(RecordFormState())
    val form: StateFlow<RecordFormState> = _form.asStateFlow()

    val categories by lazy {
        categoryDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val projects by lazy {
        projectDao.observeActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private val _uiEvent = Channel<RecordUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun startNew() {
        _form.value = RecordFormState(date = LocalDate.now())
    }

    fun loadForEdit(entryId: String) {
        viewModelScope.launch {
            val e = entryDao.getByIdOnce(entryId) ?: return@launch
            val cat = categories.value.find { it.id == e.categoryId }
            _form.value = RecordFormState(
                editingId = e.id,
                date = e.date,
                parentCategoryId = cat?.parentId,
                categoryId = e.categoryId,
                projectId = e.projectId,
                durationMin = e.durationMin,
                title = e.title,
                note = e.note ?: "",
                originalStartMinOfDay = e.startMinOfDay,
                originalCreatedAt = e.createdAt
            )
        }
    }

    fun setDate(date: LocalDate)   = update { it.copy(date = date) }
    fun selectParent(id: String)   = update { it.copy(parentCategoryId = id, categoryId = null) }
    fun selectCategory(id: String) = update { it.copy(categoryId = id) }
    fun selectProject(id: String?) = update { it.copy(projectId = id) }
    fun setDuration(min: Int)      = update { it.copy(durationMin = min.coerceIn(1, 1440)) }
    fun setTitle(t: String)        = update { it.copy(title = t) }
    fun setNote(n: String)         = update { it.copy(note = n) }

    fun save() {
        viewModelScope.launch {
            saveMutex.withLock {
                val state = _form.value
                val catId = state.categoryId
                if (catId == null) {
                    _uiEvent.send(RecordUiEvent.Toast("请选择分类"))
                    return@withLock
                }
                val dayStart = settingDao
                    .getByKey(DefaultDataSeeder.KEY_DAY_START_MIN)
                    ?.toIntOrNull() ?: 480
                val startMinOfDay = if (state.editingId != null) {
                    state.originalStartMinOfDay
                } else {
                    val maxEnd = entryDao.getMaxEndMinOfDay(state.date)
                    if (maxEnd == 0) dayStart else maxEnd
                }
                if (startMinOfDay + state.durationMin > 1440) {
                    _uiEvent.send(RecordUiEvent.Toast("今日已排满，无法添加"))
                    return@withLock
                }
                val title = state.title.ifBlank {
                    categories.value.find { it.id == catId }?.name ?: "未命名"
                }
                val now = LocalDateTime.now()
                entryDao.insert(
                    TimeEntryEntity(
                        id = state.editingId ?: UUID.randomUUID().toString(),
                        date = state.date,
                        startMinOfDay = startMinOfDay,
                        durationMin = state.durationMin,
                        title = title,
                        categoryId = catId,
                        projectId = state.projectId,
                        note = state.note.ifBlank { null },
                        createdAt = state.originalCreatedAt ?: now,
                        updatedAt = now
                    )
                )
                _uiEvent.send(RecordUiEvent.Saved)
            }
        }
    }

    private fun update(block: (RecordFormState) -> RecordFormState) {
        _form.value = block(_form.value)
    }
}
