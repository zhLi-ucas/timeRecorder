package com.example.timemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.db.AppDatabase
import com.example.timemanager.data.entity.CategoryEntity
import com.example.timemanager.data.entity.TimeEntryEntity
import com.example.timemanager.util.DateRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
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

    private val _range = MutableStateFlow(StatsRange.WEEK)
    val range: StateFlow<StatsRange> = _range.asStateFlow()

    private val categoriesFlow = categoryDao.observeAll()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = _range
        .flatMapLatest { range ->
            val (from, to) = DateRange.statsRange(range, LocalDate.now())
            combine(entryDao.observeByDateRange(from, to), categoriesFlow) { entries, cats ->
                computeStats(entries, cats)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun setRange(r: StatsRange) {
        _range.value = r
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
