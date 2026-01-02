package com.example.timemanager.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.ui.screens.HomeScreen
import com.example.timemanager.ui.screens.OptionsScreen
import com.example.timemanager.ui.screens.TimerSetupScreen
import com.example.timemanager.ui.theme.TimeManagerTheme
import com.example.timemanager.viewmodel.TimerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
}

enum class Screen {
    HOME,
    TIMER_SETUP,
    OPTIONS,
    RECORDS
}

@Composable
fun AppContent() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }
    
    // Use the same ViewModel instance across screens
    val timerViewModel: TimerViewModel = viewModel()

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                onNavigateToTimer = { currentScreen = Screen.TIMER_SETUP },
                onNavigateToOptions = { currentScreen = Screen.OPTIONS },
                onNavigateToRecords = { currentScreen = Screen.RECORDS }
            )
        }
        Screen.TIMER_SETUP -> {
            TimerSetupScreen(
                viewModel = timerViewModel
            )
            BackHandler {
                currentScreen = Screen.HOME
            }
        }
        Screen.OPTIONS -> {
            OptionsScreen(
                viewModel = timerViewModel,
                onBack = { currentScreen = Screen.HOME }
            )
            BackHandler {
                currentScreen = Screen.HOME
            }
        }
        Screen.RECORDS -> {
            TimeRecordsScreen(
                viewModel = timerViewModel,
                onBack = { currentScreen = Screen.HOME }
            )
            BackHandler {
                currentScreen = Screen.HOME
            }
        }
    }
}