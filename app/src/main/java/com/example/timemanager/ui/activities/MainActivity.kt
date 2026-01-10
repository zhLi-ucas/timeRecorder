package com.example.timemanager.ui.activities

import android.content.Intent
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.ui.screens.HomeScreen
import com.example.timemanager.ui.screens.OptionsScreen
import com.example.timemanager.ui.screens.TimeRecordsScreen
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
    OPTIONS,
    RECORDS
}

@Composable
fun AppContent() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as android.app.Application
    
    // Use the Application-scoped ViewModel instance
    val timerViewModel: TimerViewModel = viewModel(
        viewModelStoreOwner = application as androidx.lifecycle.ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                onNavigateToTimer = {
                    val intent = Intent(context, AmbientDisplayActivity::class.java)
                    context.startActivity(intent)
                },
                onNavigateToOptions = { currentScreen = Screen.OPTIONS },
                onNavigateToRecords = { currentScreen = Screen.RECORDS },
                viewModel = timerViewModel
            )
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
