package com.example.timemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.DefaultDataSeeder
import com.example.timemanager.data.db.AppDatabase
import com.example.timemanager.data.entity.AppSettingEntity
import com.example.timemanager.data.entity.ReviewEntity
import com.example.timemanager.data.remote.AiReviewRequestBuilder
import com.example.timemanager.data.remote.DeepSeekApi
import com.example.timemanager.data.remote.DeepSeekConfig
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
    private val db = AppDatabase.getInstance(application)
    private val reviewDao = db.reviewDao()
    private val entryDao = db.timeEntryDao()
    private val categoryDao = db.categoryDao()
    private val settingDao = db.appSettingDao()

    private val _periodType = MutableStateFlow(ReviewPeriod.MONTH)
    val periodType: StateFlow<ReviewPeriod> = _periodType.asStateFlow()

    private val _form = MutableStateFlow(ReviewFormState())
    val form: StateFlow<ReviewFormState> = _form.asStateFlow()

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    private val _aiConfirmed = MutableStateFlow(false)
    val aiConfirmed: StateFlow<Boolean> = _aiConfirmed.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<ReviewEntity>> = _periodType
        .flatMapLatest { type -> reviewDao.observeByType(type.name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadCurrentPeriodForm()
        viewModelScope.launch {
            _aiConfirmed.value = settingDao.getByKey(DefaultDataSeeder.KEY_AI_CONFIRMED) == "true"
        }
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

    fun loadPeriodForDate(date: LocalDate) {
        val type = _periodType.value
        val (start, end) = DateRange.currentFor(type, date)
        loadPeriod(start, end)
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

    fun setAiConfirmed() {
        _aiConfirmed.value = true
        viewModelScope.launch {
            settingDao.upsert(AppSettingEntity(DefaultDataSeeder.KEY_AI_CONFIRMED, "true"))
        }
    }

    fun resetAiState() {
        if (_aiState.value !is AiState.Loading) _aiState.value = AiState.Idle
    }

    fun generateWithAi() {
        if (_aiState.value is AiState.Loading) return
        _aiState.value = AiState.Loading
        viewModelScope.launch {
            val key = settingDao.getByKey(DefaultDataSeeder.KEY_DEEPSEEK_API_KEY)?.trim()
            if (key.isNullOrBlank()) {
                _aiState.value = AiState.Error("未配置 API key，请去设置 → AI / DeepSeek")
                return@launch
            }
            val model = settingDao.getByKey(DefaultDataSeeder.KEY_DEEPSEEK_MODEL)
                ?.ifBlank { null }
                ?: DefaultDataSeeder.DEFAULT_DEEPSEEK_MODEL
            val cfg = DeepSeekConfig(apiKey = key, model = model)

            val period = _periodType.value
            val today = LocalDate.now()
            val currentRange = DateRange.currentFor(period, today)
            val previousRange = DateRange.previousFor(period, today)

            val entries = entryDao.getByDateRange(currentRange.first, currentRange.second)
            val prevEntries = entryDao.getByDateRange(previousRange.first, previousRange.second)
            val categories = categoryDao.getAll()
            val previousReview = reviewDao.getByPeriod(period.name, previousRange.first, previousRange.second)
            val dayStart = settingDao.getByKey(DefaultDataSeeder.KEY_DAY_START_MIN)?.toIntOrNull() ?: 480

            if (entries.isEmpty()) {
                _aiState.value = AiState.Error("本${period.label}暂无时间记录，无法生成")
                return@launch
            }

            val messages = AiReviewRequestBuilder.buildMessages(
                period, currentRange, previousRange,
                entries, prevEntries, categories, previousReview, dayStart
            )
            when (val r = DeepSeekApi(cfg).chat(messages, jsonMode = true, maxTokens = 2000)) {
                is DeepSeekApi.Result.Success -> {
                    val parsed = AiReviewRequestBuilder.parseResponse(r.content)
                    if (parsed == null) {
                        _aiState.value = AiState.Error("AI 响应解析失败")
                    } else {
                        _form.value = _form.value.copy(
                            summaryText = parsed.summary,
                            mainFindings = parsed.findings,
                            adjustmentPlan = parsed.adjust
                        )
                        _aiState.value = AiState.Done
                    }
                }
                is DeepSeekApi.Result.Error ->
                    _aiState.value = AiState.Error("API key 无效或网络错误：${r.message}")
            }
        }
    }

    private fun update(block: (ReviewFormState) -> ReviewFormState) {
        _form.value = block(_form.value)
    }
}

sealed interface AiState {
    data object Idle : AiState
    data object Loading : AiState
    data object Done : AiState
    data class Error(val message: String) : AiState
}
