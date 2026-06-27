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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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

data class MonthWeekOption(
    val monday: LocalDate,
    val sunday: LocalDate,
    val review: ReviewEntity?,
    val selected: Boolean
)

/**
 * 生成前的预览快照——保留用户在 PreviewDialog 里看到的内容对应的参数，
 * 确认后 proceedAiCall 用同一组参数，"所见即所传"。
 */
data class PendingPreviewData(
    val period: ReviewPeriod,
    val anchor: LocalDate,
    val selectedMondays: List<LocalDate>?,
    val previewText: String
)

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

    private val _monthPicker = MutableStateFlow<List<MonthWeekOption>?>(null)
    val monthPicker: StateFlow<List<MonthWeekOption>?> = _monthPicker.asStateFlow()

    private val _pendingPreview = MutableStateFlow<PendingPreviewData?>(null)
    val pendingPreview: StateFlow<PendingPreviewData?> = _pendingPreview.asStateFlow()

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

    // ==================== AI 生成 ====================

    fun generateWithAi() {
        if (_aiState.value is AiState.Loading) return
        if (_pendingPreview.value != null) return    // 已在预览确认阶段
        val period = _periodType.value
        val anchor = _form.value.periodStart

        if (period == ReviewPeriod.MONTH) {
            // MONTH 走 dialog 让用户勾选本周别周报，选完后再走预览
            viewModelScope.launch {
                val options = buildMonthWeekOptions(anchor)
                if (options.none { it.review != null }) {
                    _aiState.value = AiState.Error("本月没有任何周复盘，请先在 REVIEW tab 写至少一周")
                    return@launch
                }
                _monthPicker.value = options
            }
            return
        }
        // DAY/WEEK：直接走预览中转，用户在 dialog 确认后才真正发请求
        buildAndShowPendingPreview(period, anchor, selectedMondays = null)
    }

    fun setMonthWeekSelected(monday: LocalDate, selected: Boolean) {
        val current = _monthPicker.value ?: return
        _monthPicker.value = current.map { opt ->
            if (opt.monday == monday && opt.review != null) opt.copy(selected = selected) else opt
        }
    }

    fun toggleAllMonthWeeks(selectAll: Boolean) {
        val current = _monthPicker.value ?: return
        _monthPicker.value = current.map { opt ->
            if (opt.review != null) opt.copy(selected = selectAll) else opt
        }
    }

    fun cancelMonthPicker() {
        _monthPicker.value = null
        _aiState.value = AiState.Idle
    }

    fun confirmMonthPicker() {
        val selected = _monthPicker.value
            ?.filter { it.selected }
            ?.map { it.monday }
            ?: emptyList()
        _monthPicker.value = null
        if (selected.isEmpty()) {
            _aiState.value = AiState.Error("未勾选任何周复盘")
            return
        }
        val period = _periodType.value
        val anchor = _form.value.periodStart
        // 选完周后再走预览中转，让用户确认实际选了哪些
        buildAndShowPendingPreview(period, anchor, selected)
    }

    private fun buildAndShowPendingPreview(
        period: ReviewPeriod,
        anchor: LocalDate,
        selectedMondays: List<LocalDate>?
    ) {
        viewModelScope.launch {
            val previewText = buildPreviewString(period, anchor, selectedMondays)
            _pendingPreview.value = PendingPreviewData(period, anchor, selectedMondays, previewText)
        }
    }

    fun confirmGenerate() {
        val pending = _pendingPreview.value ?: return
        _pendingPreview.value = null
        proceedAiCall(pending.period, pending.anchor, pending.selectedMondays)
    }

    fun cancelPendingPreview() {
        _pendingPreview.value = null
    }

    private fun proceedAiCall(
        period: ReviewPeriod,
        anchor: LocalDate,
        selectedMondays: List<LocalDate>?
    ) {
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
            val userPrompt = settingDao.getByKey(DefaultDataSeeder.KEY_DEEPSEEK_PROMPT)
                ?.ifBlank { null }
                ?: DefaultDataSeeder.DEFAULT_DEEPSEEK_PROMPT
            val cfg = DeepSeekConfig(apiKey = key, model = model)

            val currentRange = DateRange.currentFor(period, anchor)
            val entries = entryDao.getByDateRange(currentRange.first, currentRange.second)
            if (entries.isEmpty()) {
                _aiState.value = AiState.Error("本${period.label}暂无时间记录，无法生成")
                return@launch
            }
            val categories = categoryDao.getAll()
            val dayStart = settingDao.getByKey(DefaultDataSeeder.KEY_DAY_START_MIN)?.toIntOrNull() ?: 480
            val recentReviews = fetchRecentReviews(period, anchor, selectedMondays)

            val periodWord = when (period) {
                ReviewPeriod.DAY -> "日"
                ReviewPeriod.WEEK -> "周"
                ReviewPeriod.MONTH -> "月"
            }
            val effectivePrompt = userPrompt.replace("{period}", periodWord)
            val messages = AiReviewRequestBuilder.buildMessages(
                period, currentRange, entries, categories, recentReviews, dayStart, effectivePrompt
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

    /**
     * 预览：不发请求，构造完整的 system + user JSON 文本。
     * Settings 子页用当前真实数据（今天，若空则最近有数据日）。
     */
    suspend fun buildPreviewString(
        period: ReviewPeriod,
        anchor: LocalDate,
        selectedMondays: List<LocalDate>? = null
    ): String {
        val model = settingDao.getByKey(DefaultDataSeeder.KEY_DEEPSEEK_MODEL)
            ?.ifBlank { null }
            ?: DefaultDataSeeder.DEFAULT_DEEPSEEK_MODEL
        val userPrompt = settingDao.getByKey(DefaultDataSeeder.KEY_DEEPSEEK_PROMPT)
            ?.ifBlank { null }
            ?: DefaultDataSeeder.DEFAULT_DEEPSEEK_PROMPT

        val currentRange = DateRange.currentFor(period, anchor)
        val entries = entryDao.getByDateRange(currentRange.first, currentRange.second)
        val categories = categoryDao.getAll()
        val dayStart = settingDao.getByKey(DefaultDataSeeder.KEY_DAY_START_MIN)?.toIntOrNull() ?: 480
        val recentReviews = fetchRecentReviews(period, anchor, selectedMondays)

        val periodWord = when (period) {
            ReviewPeriod.DAY -> "日"
            ReviewPeriod.WEEK -> "周"
            ReviewPeriod.MONTH -> "月"
        }
        val effectivePrompt = userPrompt.replace("{period}", periodWord)
        return AiReviewRequestBuilder.buildPreview(
            period, currentRange, entries, categories, recentReviews, dayStart, effectivePrompt, model
        )
    }

    /**
     * REVIEW 屏不再有独立预览按钮——预览已并入 generateWithAi 流程作为二次确认。
     * Settings 子页用自己的 SettingsViewModel.requestPreview / clearPreview。
     */

    private suspend fun fetchRecentReviews(
        period: ReviewPeriod,
        anchor: LocalDate,
        selectedMondays: List<LocalDate>?
    ): List<ReviewEntity> = when (period) {
        ReviewPeriod.DAY -> {
            val n = settingDao.getByKey(DefaultDataSeeder.KEY_AI_CONTEXT_DAY_DAYS)?.toIntOrNull() ?: 3
            (0 until n).mapNotNull { offset ->
                val d = anchor.minusDays(offset.toLong())
                reviewDao.getByPeriod(ReviewPeriod.DAY.name, d, d)
            }
        }
        ReviewPeriod.WEEK -> {
            val n = settingDao.getByKey(DefaultDataSeeder.KEY_AI_CONTEXT_WEEK_DAYS)?.toIntOrNull() ?: 7
            val monday = anchor.with(DayOfWeek.MONDAY)
            val daysFromMonday = ChronoUnit.DAYS.between(monday, anchor).toInt() + 1
            val effectiveN = minOf(n, daysFromMonday)
            (0 until effectiveN).mapNotNull { offset ->
                val d = anchor.minusDays(offset.toLong())
                reviewDao.getByPeriod(ReviewPeriod.DAY.name, d, d)
            }
        }
        ReviewPeriod.MONTH -> {
            val mondays = selectedMondays ?: buildMonthWeekOptions(anchor)
                .filter { it.review != null }
                .map { it.monday }
            mondays.mapNotNull { monday ->
                reviewDao.getByPeriod(ReviewPeriod.WEEK.name, monday, monday.plusDays(6))
            }
        }
    }

    private suspend fun buildMonthWeekOptions(anchor: LocalDate): List<MonthWeekOption> {
        val monthStart = anchor.withDayOfMonth(1)
        val daysInMonth = anchor.lengthOfMonth()
        val mondays = (0 until daysInMonth).mapNotNull { offset ->
            val d = monthStart.plusDays(offset.toLong())
            d.with(DayOfWeek.MONDAY)
        }.distinct()
        return mondays.map { monday ->
            val sunday = monday.plusDays(6)
            val review = reviewDao.getByPeriod(ReviewPeriod.WEEK.name, monday, sunday)
            MonthWeekOption(monday, sunday, review, selected = review != null)
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
