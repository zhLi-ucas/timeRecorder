package com.example.timemanager.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.DurationRepository
import com.example.timemanager.data.HealthRecord
import com.example.timemanager.data.HealthRecordRepository
import com.example.timemanager.data.Tag
import com.example.timemanager.data.TagRepository
import com.example.timemanager.data.Task
import com.example.timemanager.data.TimeRecord
import com.example.timemanager.data.TimeRecordRepository
import com.example.timemanager.data.TimerState
import com.example.timemanager.service.NotificationService
import com.example.timemanager.ui.components.ReminderType
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val tagRepository = TagRepository(application)
    private val durationRepository = DurationRepository(application)
    private val timeRecordRepository = TimeRecordRepository(application)
    private val healthRecordRepository = HealthRecordRepository(application)
    private val prefs = application.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_START_TIME = "ongoing_start_time"
        private const val KEY_TAG_NAME = "ongoing_tag_name"
        private const val KEY_DESCRIPTION = "ongoing_description"
        private const val KEY_CREATION_TYPE = "ongoing_creation_type"
        
        // Reminder Keys
        private const val KEY_WATER_INTERVAL = "water_interval_mins"
        private const val KEY_STAND_INTERVAL = "stand_interval_mins"
        private const val KEY_WATER_START_TIME = "water_start_time"
        private const val KEY_STAND_START_TIME = "stand_start_time"
        
        private const val DEFAULT_WATER_INTERVAL = 30
        private const val DEFAULT_STAND_INTERVAL = 50
    }

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    val displayTags: StateFlow<List<Tag>> = _tags.map { tags ->
        tags.filter { it.showOnHome }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val otherTags: StateFlow<List<Tag>> = _tags.map { tags ->
        tags.filter { !it.showOnHome }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSelectedTag = MutableStateFlow<Tag?>(null)
    val currentSelectedTag: StateFlow<Tag?> = _currentSelectedTag.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun selectTag(tag: Tag) {
        _currentSelectedTag.value = tag
    }

    private val _durations = MutableStateFlow<List<Int>>(emptyList())
    val durations: StateFlow<List<Int>> = _durations.asStateFlow()

    private val _records = MutableStateFlow<List<TimeRecord>>(emptyList())
    val records: StateFlow<List<TimeRecord>> = _records.asStateFlow()

    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _displaySeconds = MutableStateFlow(0)
    val displaySeconds: StateFlow<Int> = _displaySeconds.asStateFlow()

    // Reminder State
    private val _waterInterval = MutableStateFlow(DEFAULT_WATER_INTERVAL)
    val waterInterval: StateFlow<Int> = _waterInterval.asStateFlow()

    private val _standInterval = MutableStateFlow(DEFAULT_STAND_INTERVAL)
    val standInterval: StateFlow<Int> = _standInterval.asStateFlow()
    
    // Progress: 1.0 = Full, 0.0 = Empty
    private val _waterProgress = MutableStateFlow(0f) 
    val waterProgress: StateFlow<Float> = _waterProgress.asStateFlow()
    
    private val _standProgress = MutableStateFlow(0f)
    val standProgress: StateFlow<Float> = _standProgress.asStateFlow()

    private var timerJob: Job? = null
    private var reminderJob: Job? = null

    init {
        loadTags()
        loadDurations()
        loadRecords()
        checkOngoingTask()
        loadReminderSettings()
        startReminderLoop()
    }

    private fun loadTags() {
        viewModelScope.launch {
            _tags.value = tagRepository.getTags()
        }
    }

    private fun loadDurations() {
        viewModelScope.launch {
            _durations.value = durationRepository.getDurations()
        }
    }

    fun loadRecords() {
        viewModelScope.launch {
            _records.value = timeRecordRepository.getAllRecords()
        }
    }

    fun addRecord(record: TimeRecord) {
        timeRecordRepository.addRecord(record)
        loadRecords()
    }

    fun updateRecord(record: TimeRecord) {
        timeRecordRepository.updateRecord(record)
        loadRecords()
    }

    fun deleteRecord(recordId: String) {
        timeRecordRepository.deleteRecord(recordId)
        loadRecords()
    }

    fun addTag(name: String) {
        val newTag = Tag.create(name)
        tagRepository.addTag(newTag)
        loadTags()
    }

    fun deleteTag(tag: Tag) {
        tagRepository.removeTag(tag)
        loadTags()
    }

    fun updateTag(tag: Tag) {
        tagRepository.updateTag(tag)
        loadTags()
    }

    fun addDuration(minutes: Int) {
        durationRepository.addDuration(minutes)
        loadDurations()
    }

    fun deleteDuration(minutes: Int) {
        durationRepository.removeDuration(minutes)
        loadDurations()
    }

    // --- New Logic: Start Point based Task Recording ---

    private fun checkOngoingTask() {
        val startTime = prefs.getLong(KEY_START_TIME, -1L)
        if (startTime != -1L) {
            val tagName = prefs.getString(KEY_TAG_NAME, "") ?: ""
            val description = prefs.getString(KEY_DESCRIPTION, "") ?: ""
            
            // Reconstruct task state
            val task = Task(
                tag = tagName, // Fixed: Pass String directly
                durationMinutes = 0, // Not relevant for infinite timer
                isStopwatch = true,
                startTime = startTime,
                description = description
            )
            
            _currentTask.value = task
            _timerState.value = TimerState.RUNNING
            startUiTicker(startTime)
        } else {
            _timerState.value = TimerState.IDLE
            _currentTask.value = null
            _displaySeconds.value = 0
        }
    }

    fun startTask(tag: Tag? = null, description: String = "", creationType: String = "NORMAL") {
        val targetTag = tag ?: _currentSelectedTag.value ?: return

        val startTime = System.currentTimeMillis()
        
        // Persist state
        prefs.edit().apply {
            putLong(KEY_START_TIME, startTime)
            putString(KEY_TAG_NAME, targetTag.name)
            putString(KEY_DESCRIPTION, description)
            putString(KEY_CREATION_TYPE, creationType)
            apply()
        }

        val task = Task(
            tag = targetTag.name, // Fixed: Pass String directly
            durationMinutes = 0,
            isStopwatch = true,
            startTime = startTime,
            description = description
        )

        _currentTask.value = task
        _timerState.value = TimerState.RUNNING
        startUiTicker(startTime)
    }

    fun endTask() {
        val startTime = prefs.getLong(KEY_START_TIME, -1L)
        if (startTime == -1L) return

        val tagName = prefs.getString(KEY_TAG_NAME, "") ?: ""
        val description = prefs.getString(KEY_DESCRIPTION, "") ?: ""
        val creationType = prefs.getString(KEY_CREATION_TYPE, "NORMAL") ?: "NORMAL"
        val endTime = System.currentTimeMillis()
        val durationSeconds = (endTime - startTime) / 1000

        if (durationSeconds > 0) {
            val record = TimeRecord(
                tag = tagName,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds,
                description = description,
                creationType = creationType
            )
            // Trigger UI event instead of saving immediately
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowSaveRecordDialog(record))
            }
        }

        // Clear state
        prefs.edit().clear().apply()
        
        stopUiTicker()
        _timerState.value = TimerState.IDLE
        _currentTask.value = null
        _displaySeconds.value = 0
    }

    private fun startUiTicker(startTime: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val seconds = (now - startTime) / 1000
                _displaySeconds.value = seconds.toInt()
                delay(1000)
            }
        }
    }

    private fun stopUiTicker() {
        timerJob?.cancel()
    }

    // --- Reminder Logic ---

    private fun loadReminderSettings() {
        _waterInterval.value = prefs.getInt(KEY_WATER_INTERVAL, DEFAULT_WATER_INTERVAL)
        _standInterval.value = prefs.getInt(KEY_STAND_INTERVAL, DEFAULT_STAND_INTERVAL)
    }

    fun setReminderInterval(type: ReminderType, minutes: Int) {
        val key = when (type) {
            ReminderType.WATER -> KEY_WATER_INTERVAL
            ReminderType.STAND -> KEY_STAND_INTERVAL
        }
        prefs.edit().putInt(key, minutes).apply()
        loadReminderSettings()
        // Reset the specific timer when settings change? Maybe just let it adapt.
    }

    fun resetReminder(type: ReminderType) {
        val key = when (type) {
            ReminderType.WATER -> KEY_WATER_START_TIME
            ReminderType.STAND -> KEY_STAND_START_TIME
        }
        // Set start time to NOW (Full)
        // If not started (0), this starts it.
        // If already started, this resets it to full.
        prefs.edit().putLong(key, System.currentTimeMillis()).apply()
        updateReminderProgress()
        recordHealthEvent(type)
    }

    fun recordHealthEvent(type: ReminderType) {
        val record = HealthRecord(
            type = type.name,
            timestamp = System.currentTimeMillis()
        )
        healthRecordRepository.addRecord(record)
    }

    private fun startReminderLoop() {
        reminderJob?.cancel()
        reminderJob = viewModelScope.launch {
            while (true) {
                updateReminderProgress()
                delay(10000) // Update every 10 seconds to save battery, precision isn't critical
            }
        }
    }

    private fun updateReminderProgress() {
        val now = System.currentTimeMillis()
        
        // Water
        val waterStart = prefs.getLong(KEY_WATER_START_TIME, 0L)
        if (waterStart == 0L) {
            _waterProgress.value = 0f // Gray/Empty
        } else {
            val waterDurationMs = _waterInterval.value * 60 * 1000L
            val elapsed = now - waterStart
            val remaining = (waterDurationMs - elapsed).coerceAtLeast(0)
            _waterProgress.value = (remaining.toFloat() / waterDurationMs).coerceIn(0f, 1f)
            
            if (remaining == 0L && elapsed < waterDurationMs + 20000) { // Notify just once (approx)
                 // Notification Logic handled here or separately?
                 // Simple check: if we just crossed 0
                 // To avoid spam, we can store a flag or just rely on user resetting it.
                 // For now, let's trigger notification if it's exactly 0 (or close enough in our loop)
                 // Better: Check if we haven't notified yet.
                 // Simplification: We will just show progress. Notification triggers need a state "Notified".
                 checkAndNotify(ReminderType.WATER, now)
            }
        }

        // Stand
        val standStart = prefs.getLong(KEY_STAND_START_TIME, 0L)
        if (standStart == 0L) {
            _standProgress.value = 0f
        } else {
            val standDurationMs = _standInterval.value * 60 * 1000L
            val elapsed = now - standStart
            val remaining = (standDurationMs - elapsed).coerceAtLeast(0)
            _standProgress.value = (remaining.toFloat() / standDurationMs).coerceIn(0f, 1f)
            
            if (remaining == 0L && elapsed < standDurationMs + 20000) {
                 checkAndNotify(ReminderType.STAND, now)
            }
        }
    }
    
    private fun checkAndNotify(type: ReminderType, now: Long) {
        val lastNotifyKey = "last_notify_${type.name}"
        val lastNotifyTime = prefs.getLong(lastNotifyKey, 0L)
        
        // Only notify if we haven't notified in the last minute (debounce)
        if (now - lastNotifyTime > 60000) {
             NotificationService.showReminderNotification(getApplication(), type)
             prefs.edit().putLong(lastNotifyKey, now).apply()
        }
    }

    fun findFreeTimeSlot(targetTime: Long): Pair<Long, Long>? {
        val records = _records.value.sortedBy { it.startTime }
        
        // 1. Check if targetTime is occupied
        val occupied = records.any { targetTime >= it.startTime && targetTime < it.endTime }
        if (occupied) return null
        
        // 2. Find the gap
        // We need to define the day boundaries for targetTime.
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = targetTime
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val dayStart = calendar.timeInMillis
        
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val dayEnd = calendar.timeInMillis
        
        // Filter records for this day (overlapping with the day)
        val dayRecords = records.filter { it.endTime > dayStart && it.startTime < dayEnd }
            .sortedBy { it.startTime }
        
        // Find gap
        var lastEnd = dayStart
        for (record in dayRecords) {
            val start = record.startTime.coerceAtLeast(dayStart)
            val end = record.endTime.coerceAtMost(dayEnd)
            
            // If the record starts after the last end, there is a gap [lastEnd, start]
            if (targetTime < start) {
                // Check if targetTime falls in this gap
                if (targetTime >= lastEnd) {
                    return Pair(lastEnd, start)
                }
            }
            lastEnd = end.coerceAtLeast(lastEnd)
        }
        
        // Check last gap (after last record until dayEnd)
        if (targetTime >= lastEnd && targetTime < dayEnd) {
            return Pair(lastEnd, dayEnd)
        }
        
        return null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        reminderJob?.cancel()
    }
}

sealed interface UiEvent {
    data class ShowSaveRecordDialog(val record: TimeRecord) : UiEvent
}
