package com.example.timemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.DurationRepository
import com.example.timemanager.data.Tag
import com.example.timemanager.data.TagRepository
import com.example.timemanager.data.Task
import com.example.timemanager.data.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val tagRepository = TagRepository(application)
    private val durationRepository = DurationRepository(application)

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _durations = MutableStateFlow<List<Int>>(emptyList())
    val durations: StateFlow<List<Int>> = _durations.asStateFlow()

    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _displaySeconds = MutableStateFlow(0)
    val displaySeconds: StateFlow<Int> = _displaySeconds.asStateFlow()
    // Alias for compatibility if needed, but better to update UI
    val remainingSeconds: StateFlow<Int> = _displaySeconds.asStateFlow()

    private val _onTimerCompleted = MutableStateFlow(false)
    val onTimerCompleted: StateFlow<Boolean> = _onTimerCompleted.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadTags()
        loadDurations()
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

    fun startTimer(task: Task) {
        _currentTask.value = task
        _timerState.value = TimerState.RUNNING
        
        if (task.isStopwatch) {
            _displaySeconds.value = 0
        } else {
            _displaySeconds.value = task.durationMinutes * 60
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            if (task.isStopwatch) {
                while (_timerState.value == TimerState.RUNNING) {
                    delay(1000)
                    _displaySeconds.value = _displaySeconds.value + 1
                }
            } else {
                while (_displaySeconds.value > 0 && _timerState.value == TimerState.RUNNING) {
                    delay(1000)
                    _displaySeconds.value = _displaySeconds.value - 1
                }

                if (_displaySeconds.value <= 0) {
                    _timerState.value = TimerState.COMPLETED
                    _onTimerCompleted.value = true
                }
            }
        }
    }

    fun pauseTimer() {
        if (_timerState.value == TimerState.RUNNING) {
            _timerState.value = TimerState.PAUSED
            timerJob?.cancel()
        }
    }

    fun resumeTimer() {
        if (_timerState.value == TimerState.PAUSED) {
            _timerState.value = TimerState.RUNNING
            val isStopwatch = _currentTask.value?.isStopwatch == true
            
            timerJob = viewModelScope.launch {
                if (isStopwatch) {
                    while (_timerState.value == TimerState.RUNNING) {
                        delay(1000)
                        _displaySeconds.value = _displaySeconds.value + 1
                    }
                } else {
                    while (_displaySeconds.value > 0 && _timerState.value == TimerState.RUNNING) {
                        delay(1000)
                        _displaySeconds.value = _displaySeconds.value - 1
                    }

                    if (_displaySeconds.value <= 0) {
                        _timerState.value = TimerState.COMPLETED
                        _onTimerCompleted.value = true
                    }
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.IDLE
        _currentTask.value = null
        _displaySeconds.value = 0
    }

    fun resetTimer() {
        stopTimer()
    }

    fun resetTimerCompletedFlag() {
        _onTimerCompleted.value = false
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

