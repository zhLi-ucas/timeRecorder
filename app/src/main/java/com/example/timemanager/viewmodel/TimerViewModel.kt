package com.example.timemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanager.data.Task
import com.example.timemanager.data.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _onTimerCompleted = MutableStateFlow(false)
    val onTimerCompleted: StateFlow<Boolean> = _onTimerCompleted.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer(task: Task) {
        _currentTask.value = task
        _timerState.value = TimerState.RUNNING
        _remainingSeconds.value = task.durationMinutes * 60

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0 && _timerState.value == TimerState.RUNNING) {
                delay(1000)
                _remainingSeconds.value = _remainingSeconds.value - 1
            }

            if (_remainingSeconds.value <= 0) {
                _timerState.value = TimerState.COMPLETED
                _onTimerCompleted.value = true
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
            timerJob = viewModelScope.launch {
                while (_remainingSeconds.value > 0 && _timerState.value == TimerState.RUNNING) {
                    delay(1000)
                    _remainingSeconds.value = _remainingSeconds.value - 1
                }

                if (_remainingSeconds.value <= 0) {
                    _timerState.value = TimerState.COMPLETED
                    _onTimerCompleted.value = true
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.IDLE
        _currentTask.value = null
        _remainingSeconds.value = 0
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

