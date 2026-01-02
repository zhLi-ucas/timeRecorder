package com.example.timemanager.ui.screens

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.service.NotificationService
import com.example.timemanager.TimeManagerApplication
import com.example.timemanager.viewmodel.TimerViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AmbientDisplayScreen(
    viewModel: TimerViewModel? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val actualViewModel: TimerViewModel = viewModel ?: viewModel(
        viewModelStoreOwner = context.applicationContext as androidx.lifecycle.ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )

    val currentTask by actualViewModel.currentTask.collectAsState()
    val remainingSeconds by actualViewModel.remainingSeconds.collectAsState()
    val timerState by actualViewModel.timerState.collectAsState()
    val onTimerCompleted by actualViewModel.onTimerCompleted.collectAsState()

    // 监听计时结束事件
    LaunchedEffect(onTimerCompleted) {
        if (onTimerCompleted && currentTask != null) {
            NotificationService.showTimerCompletedNotification(
                context,
                currentTask!!.tag,
                currentTask!!.description
            )
            actualViewModel.resetTimerCompletedFlag()
        }
    }

    // 当前时间
    val currentTime = remember {
        mutableStateOf(getCurrentTime())
    }
    val currentDate = remember {
        mutableStateOf(getCurrentDate())
    }

    // 每秒更新时间
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime.value = getCurrentTime()
            currentDate.value = getCurrentDate()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(64.dp)
        ) {
            // 左侧：时间与日期
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 当前时间
                Text(
                    text = currentTime.value,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // 日期
                Text(
                    text = currentDate.value,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // 右侧：当前任务与倒计时
            if (currentTask != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(IntrinsicSize.Max)
                ) {
                    Text(
                        text = currentTask!!.tag,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    if (currentTask!!.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentTask!!.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 倒计时
                    if (timerState == com.example.timemanager.data.TimerState.RUNNING ||
                        timerState == com.example.timemanager.data.TimerState.PAUSED) {
                        Text(
                            text = formatRemainingTime(remainingSeconds),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (timerState == com.example.timemanager.data.TimerState.COMPLETED) {
                        Text(
                            text = "时间到！",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "暂无任务",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

private fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault())
    return sdf.format(Date())
}

private fun formatRemainingTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
        else -> String.format("%02d:%02d", minutes, secs)
    }
}

