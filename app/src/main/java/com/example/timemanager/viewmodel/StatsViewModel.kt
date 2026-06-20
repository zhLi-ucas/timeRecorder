package com.example.timemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.db.AppDatabase
import com.example.timemanager.data.entity.CategoryEntity
import com.example.timemanager.data.entity.TimeEntryEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
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

    private val _range = MutableStateFlow(StatsRange.MONTH)
    val range: StateFlow<StatsRange> = _range.asStateFlow()

    private val categoriesFlow = categoryDao.observeAll()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = _range
        .flatMapLatest { range ->
            val (from, to) = rangeToDates(range, LocalDate.now())
            combine(entryDao.observeByDateRange(from, to), categoriesFlow) { entries, cats ->
                computeStats(entries, cats)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun setRange(r: StatsRange) {
        _range.value = r
    }

    private fun rangeToDates(range: StatsRange, today: LocalDate): Pair<LocalDate, LocalDate> =
        when (range) {
            StatsRange.TODAY -> today to today
            StatsRange.WEEK -> {
                val monday = today.with(DayOfWeek.MONDAY)
                monday to monday.plusDays(6)
            }
            StatsRange.MONTH -> today.withDayOfMonth(1) to
                today.withDayOfMonth(today.lengthOfMonth())
            StatsRange.YEAR -> LocalDate.of(today.year, 1, 1) to
                LocalDate.of(today.year, 12, 31)
        }

    private fun computeStats(
        entries: List<TimeEntryEntity>,
        cats: List<CategoryEntity>
    ): StatsUiState {
        val byId = cats.associateBy { it.id }
        val firstTotals = mutableMapOf<String, Long>()
        val secondTotals = mutableMapOf<String, Long>()
        entries.forEach { e ->
            val cur = secondTotals[e.categoryId] ?: 0L
            secondTotals[e.categoryId] = cur + e.durationMin
            val parentId = byId[e.categoryId]?.parentId
            if (parentId != null) {
                firstTotals[parentId] = (firstTotals[parentId] ?: 0L) + e.durationMin
            }
        }
        val total = entries.sumOf { it.durationMin.toLong() }
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
