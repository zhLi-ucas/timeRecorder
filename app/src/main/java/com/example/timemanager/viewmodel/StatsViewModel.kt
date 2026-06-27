package com.example.timemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.db.AppDatabase
import com.example.timemanager.data.entity.CategoryEntity
import com.example.timemanager.data.entity.ReviewEntity
import com.example.timemanager.data.entity.TimeEntryEntity
import com.example.timemanager.util.DateRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

enum class StatsRange(val label: String) {
    TODAY("今日"),
    WEEK("本周"),
    MONTH("本月"),
    YEAR("今年")
}

data class StatsUiState(
    val totalMin: Long = 0L,
    val groups: List<FirstLevelGroup> = emptyList()
)

data class FirstLevelGroup(
    val category: CategoryEntity,
    val totalMin: Long,
    val percentage: Float,
    val barFraction: Float,
    val secondLevel: List<Pair<CategoryEntity, Long>>
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val entryDao = db.timeEntryDao()
    private val categoryDao = db.categoryDao()
    private val reviewDao = db.reviewDao()

    private val _range = MutableStateFlow(StatsRange.WEEK)
    val range: StateFlow<StatsRange> = _range.asStateFlow()

    private val _selectedIdx = MutableStateFlow(0)
    val selectedIdx: StateFlow<Int> = _selectedIdx.asStateFlow()

    // anchor 记忆：用户主动选择的 anchor；entry 增删导致 validAnchors 列表变化时，
    // 用此值重算 idx，而不是按下标 → 避免被动跳页
    private val _userAnchor = MutableStateFlow<LocalDate?>(null)

    private val categoriesFlow = categoryDao.observeAll()

    @OptIn(ExperimentalCoroutinesApi::class)
    val validAnchors: StateFlow<List<LocalDate>> = _range
        .flatMapLatest { range ->
            entryDao.observeAllDates().map { dates ->
                // asc：idx=0 最旧，idx=size-1 最新；与 TODAY 屏一致——右滑看过去
                dates.map { anchorForRange(range, it) }.distinct().sorted()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val anchorDate: StateFlow<LocalDate> = combine(validAnchors, _selectedIdx) { anchors, idx ->
        anchors.getOrNull(idx) ?: LocalDate.now()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = combine(_range, anchorDate) { range, anchor -> range to anchor }
        .flatMapLatest { (range, anchor) ->
            val (from, to) = DateRange.statsRange(range, anchor)
            combine(entryDao.observeByDateRange(from, to), categoriesFlow) { entries, cats ->
                computeStats(entries, cats)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentReview: StateFlow<ReviewEntity?> = combine(_range, anchorDate) { range, anchor -> range to anchor }
        .flatMapLatest { (range, anchor) ->
            val type = when (range) {
                StatsRange.TODAY -> ReviewPeriod.DAY
                StatsRange.WEEK -> ReviewPeriod.WEEK
                StatsRange.MONTH -> ReviewPeriod.MONTH
                StatsRange.YEAR -> return@flatMapLatest flowOf<ReviewEntity?>(null)
            }
            val (start, end) = DateRange.currentFor(type, anchor)
            reviewDao.observeByPeriod(type.name, start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            validAnchors.collect { anchors ->
                if (anchors.isEmpty()) return@collect
                val latest = anchors.size - 1   // asc 排列下，最新在末端
                val target = _userAnchor.value
                val newIdx = when {
                    target == null -> latest
                    anchors.contains(target) -> anchors.indexOf(target)
                    else -> latest   // anchor 被删空，clamp 到最新
                }
                if (_selectedIdx.value != newIdx) _selectedIdx.value = newIdx
                _userAnchor.value = anchors[newIdx]
            }
        }
    }

    fun selectPage(idx: Int) {
        val anchors = validAnchors.value
        if (idx !in anchors.indices) return
        _selectedIdx.value = idx
        _userAnchor.value = anchors[idx]
    }

    fun shift(delta: Int) {
        val size = validAnchors.value.size
        if (size == 0) return
        val newIdx = (_selectedIdx.value + delta).coerceIn(0, size - 1)
        selectPage(newIdx)
    }

    fun setRange(r: StatsRange) {
        if (_range.value == r) return
        _range.value = r
        _userAnchor.value = null
    }

    private fun anchorForRange(range: StatsRange, date: LocalDate): LocalDate = when (range) {
        StatsRange.TODAY -> date
        StatsRange.WEEK -> date.with(DayOfWeek.MONDAY)
        StatsRange.MONTH -> date.withDayOfMonth(1)
        StatsRange.YEAR -> LocalDate.of(date.year, 1, 1)
    }

    private fun computeStats(
        entries: List<TimeEntryEntity>,
        cats: List<CategoryEntity>
    ): StatsUiState {
        val byId = cats.associateBy { it.id }
        val INVALID_PARENT_ID = "cat_invalid"
        val OTHER_PARENT_ID = "cat_other"

        val firstTotals = mutableMapOf<String, Long>()
        entries.forEach { e ->
            val cat = byId[e.categoryId] ?: return@forEach
            val parent = cat.parentId ?: return@forEach
            val eff = e.effectiveness.coerceIn(0, 100) / 100f
            val dur = e.durationMin.toLong()
            val invalid = (dur * (1f - eff)).toLong()
            val valid = dur - invalid

            if (parent == INVALID_PARENT_ID) {
                firstTotals[INVALID_PARENT_ID] = (firstTotals[INVALID_PARENT_ID] ?: 0L) + invalid
                firstTotals[OTHER_PARENT_ID]   = (firstTotals[OTHER_PARENT_ID] ?: 0L) + valid
            } else {
                firstTotals[parent]            = (firstTotals[parent] ?: 0L) + valid
                firstTotals[INVALID_PARENT_ID] = (firstTotals[INVALID_PARENT_ID] ?: 0L) + invalid
            }
        }

        val secondTotals = mutableMapOf<String, Long>()
        entries.forEach { e ->
            secondTotals[e.categoryId] = (secondTotals[e.categoryId] ?: 0L) + e.durationMin
        }

        val total = firstTotals.values.sum()
        val maxFirst = firstTotals.values.maxOrNull() ?: 0L
        val groups = cats.asSequence()
            .filter { it.parentId == null && !it.isArchived }
            .sortedBy { it.sortOrder }
            .map { parent ->
                val pTotal = firstTotals[parent.id] ?: 0L
                val second = cats.asSequence()
                    .filter { it.parentId == parent.id }
                    .map { c -> c to (secondTotals[c.id] ?: 0L) }
                    .filter { it.second > 0L }
                    .sortedByDescending { it.second }
                    .toList()
                FirstLevelGroup(
                    category = parent,
                    totalMin = pTotal,
                    percentage = if (total > 0) pTotal.toFloat() / total else 0f,
                    barFraction = if (maxFirst > 0) pTotal.toFloat() / maxFirst else 0f,
                    secondLevel = second
                )
            }
            .filter { it.totalMin > 0L }
            .toList()
        return StatsUiState(total, groups)
    }
}
