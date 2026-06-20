package com.example.timemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.DefaultDataSeeder
import com.example.timemanager.data.db.AppDatabase
import com.example.timemanager.data.entity.CategoryEntity
import com.example.timemanager.data.entity.TimeEntryEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TimeEntryWithCategory(
    val entry: TimeEntryEntity,
    val category: CategoryEntity?,
    val parent: CategoryEntity?
)

data class TodayUiState(
    val entries: List<TimeEntryWithCategory> = emptyList(),
    val totalMin: Long = 0L
)

sealed interface TodayUiEvent {
    data class ShowToast(val msg: String) : TodayUiEvent
}

class TodayLedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val entryDao = db.timeEntryDao()
    private val categoryDao = db.categoryDao()
    private val settingDao = db.appSettingDao()

    private val reorderMutex = Mutex()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiEvent = Channel<TodayUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val entriesFlow = _selectedDate
        .flatMapLatest { date -> entryDao.observeByDate(date) }

    private val categoriesFlow = categoryDao.observeAll()

    val uiState: StateFlow<TodayUiState> = combine(entriesFlow, categoriesFlow) { entries, cats ->
        val paired = entries.map { e ->
            val cat = cats.find { it.id == e.categoryId }
            val parent = cat?.parentId?.let { pid -> cats.find { it.id == pid } }
            TimeEntryWithCategory(e, cat, parent)
        }
        TodayUiState(paired, entries.sumOf { it.durationMin.toLong() })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    fun reorder(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        viewModelScope.launch {
            reorderMutex.withLock {
                val date = _selectedDate.value
                val current = entryDao.getByDate(date).toMutableList()
                if (fromIndex !in current.indices || toIndex !in current.indices) return@withLock

                val moved = current.removeAt(fromIndex)
                current.add(toIndex, moved)

                val dayStart = settingDao
                    .getByKey(DefaultDataSeeder.KEY_DAY_START_MIN)
                    ?.toIntOrNull() ?: 480

                var cursor = dayStart
                for (e in current) {
                    cursor += e.durationMin
                    if (cursor > 1440) {
                        _uiEvent.send(TodayUiEvent.ShowToast("今日已排满，请先调整已有条目"))
                        return@withLock
                    }
                }

                val now = LocalDateTime.now()
                cursor = dayStart
                current.forEach { e ->
                    entryDao.update(e.copy(startMinOfDay = cursor, updatedAt = now))
                    cursor += e.durationMin
                }
            }
        }
    }

    fun insertDebugEntry() {
        viewModelScope.launch {
            reorderMutex.withLock {
                val date = _selectedDate.value
                val dayStart = settingDao
                    .getByKey(DefaultDataSeeder.KEY_DAY_START_MIN)
                    ?.toIntOrNull() ?: 480
                val maxEnd = entryDao.getMaxEndMinOfDay(date)
                val start = if (maxEnd == 0) dayStart else maxEnd
                val duration = 60
                if (start + duration > 1440) {
                    _uiEvent.send(TodayUiEvent.ShowToast("今日已排满"))
                    return@withLock
                }

                val categories = categoryDao.getAll().filter { it.parentId != null }
                val cat = categories.randomOrNull()
                    ?: return@withLock

                val now = LocalDateTime.now()
                entryDao.insert(
                    TimeEntryEntity(
                        id = UUID.randomUUID().toString(),
                        date = date,
                        startMinOfDay = start,
                        durationMin = duration,
                        title = "${cat.name}（调试）",
                        categoryId = cat.id,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }
}
