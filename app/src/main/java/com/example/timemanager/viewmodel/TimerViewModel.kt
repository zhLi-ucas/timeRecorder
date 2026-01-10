package com.example.timemanager.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.DurationRepository
import com.example.timemanager.data.Tag
import com.example.timemanager.data.TagRepository
import com.example.timemanager.data.Task
import com.example.timemanager.data.TimeRecord
import com.example.timemanager.data.TimeRecordRepository
import com.example.timemanager.data.TimerState
import com.example.timemanager.service.NotificationService
import com.example.timemanager.ui.components.ReminderType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val tagRepository = TagRepository(application)
    private val durationRepository = DurationRepository(application)
    private val timeRecordRepository = TimeRecordRepository(application)
    private val prefs = application.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_START_TIME = "ongoing_start_time"
        private const val KEY_TAG_NAME = "ongoing_tag_name"
        private const val KEY_DESCRIPTION = "ongoing_description"
        
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

    fun startTask(tag: Tag, description: String) {
        val startTime = System.currentTimeMillis()
        
        // Persist state
        prefs.edit().apply {
            putLong(KEY_START_TIME, startTime)
            putString(KEY_TAG_NAME, tag.name)
            putString(KEY_DESCRIPTION, description)
            apply()
        }

        val task = Task(
            tag = tag.name, // Fixed: Pass String directly
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
        val endTime = System.currentTimeMillis()
        val durationSeconds = (endTime - startTime) / 1000

        if (durationSeconds > 0) {
            val record = TimeRecord(
                tag = tagName,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds,
                description = description
            )
            timeRecordRepository.addRecord(record)
            loadRecords()
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

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        reminderJob?.cancel()
    }
}
