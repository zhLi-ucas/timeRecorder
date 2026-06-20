package com.example.timemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.db.AppDatabase
import com.example.timemanager.data.entity.ReviewEntity
import com.example.timemanager.util.DateRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class ReviewPeriod(
    val label: String,
    val field1: String,
    val field2: String,
    val field3: String
) {
    DAY("日", "今日最有价值的时间段", "今日最大的浪费", "明天需要调整"),
    WEEK("周", "本周主要时间投入", "本周时间结构问题", "下周调整"),
    MONTH("月", "本月核心工作 + 主要成果", "本月时间偏差", "下月时间预算")
}

data class ReviewFormState(
    val editingId: String? = null,
    val periodStart: LocalDate = LocalDate.now(),
    val periodEnd: LocalDate = LocalDate.now(),
    val originalCreatedAt: LocalDateTime? = null,
    val summaryText: String = "",
    val mainFindings: String = "",
    val adjustmentPlan: String = ""
) {
    val isEditing: Boolean get() = editingId != null
}

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val reviewDao = AppDatabase.getInstance(application).reviewDao()

    private val _periodType = MutableStateFlow(ReviewPeriod.MONTH)
    val periodType: StateFlow<ReviewPeriod> = _periodType.asStateFlow()

    private val _form = MutableStateFlow(ReviewFormState())
    val form: StateFlow<ReviewFormState> = _form.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<ReviewEntity>> = _periodType
        .flatMapLatest { type -> reviewDao.observeByType(type.name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadCurrentPeriodForm()
    }

    fun selectPeriod(type: ReviewPeriod) {
        _periodType.value = type
        loadCurrentPeriodForm()
    }

    fun loadPeriod(start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            val type = _periodType.value
            val existing = reviewDao.getByPeriod(type.name, start, end)
            _form.value = if (existing != null) {
                ReviewFormState(
                    editingId = existing.id,
                    periodStart = existing.periodStart,
                    periodEnd = existing.periodEnd,
                    originalCreatedAt = existing.createdAt,
                    summaryText = existing.summaryText ?: "",
                    mainFindings = existing.mainFindings ?: "",
                    adjustmentPlan = existing.adjustmentPlan ?: ""
                )
            } else {
                ReviewFormState(periodStart = start, periodEnd = end)
            }
        }
    }

    private fun loadCurrentPeriodForm() {
        val (start, end) = DateRange.currentFor(_periodType.value, LocalDate.now())
        loadPeriod(start, end)
    }

    fun setSummary(t: String) = update { it.copy(summaryText = t) }
    fun setFindings(t: String) = update { it.copy(mainFindings = t) }
    fun setAdjust(t: String) = update { it.copy(adjustmentPlan = t) }

    fun save() {
        viewModelScope.launch {
            val state = _form.value
            val now = LocalDateTime.now()
            val entity = ReviewEntity(
                id = state.editingId ?: UUID.randomUUID().toString(),
                periodType = _periodType.value.name,
                periodStart = state.periodStart,
                periodEnd = state.periodEnd,
                summaryText = state.summaryText.ifBlank { null },
                mainFindings = state.mainFindings.ifBlank { null },
                adjustmentPlan = state.adjustmentPlan.ifBlank { null },
                createdAt = state.originalCreatedAt ?: now,
                updatedAt = now
            )
            reviewDao.insert(entity)
            loadCurrentPeriodForm()
        }
    }

    fun delete(review: ReviewEntity) {
        viewModelScope.launch {
            reviewDao.delete(review)
            loadCurrentPeriodForm()
        }
    }

    private fun update(block: (ReviewFormState) -> ReviewFormState) {
        _form.value = block(_form.value)
    }
}
