package com.example.timemanager.ui.activities

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.ui.screens.TodayLedgerScreen
import com.example.timemanager.ui.theme.TimeManagerTheme
import com.example.timemanager.viewmodel.TodayLedgerViewModel

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

enum class Screen(val label: String, val icon: ImageVector) {
    TODAY("今日账本", Icons.Filled.Home),
    RECORD("记一笔", Icons.Filled.Add),
    STATS("统计", Icons.Filled.Assessment),
    REVIEW("复盘", Icons.Filled.AutoStories),
    SETTINGS("设置", Icons.Filled.Settings),
}

@Composable
fun AppContent() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.TODAY) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = screen == currentScreen,
                        onClick = { currentScreen = screen },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentScreen) {
                Screen.TODAY -> TodayTab()
                else -> PlaceholderTab(currentScreen.label)
            }
        }
    }
}

@Composable
private fun TodayTab() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as Application
    val vm: TodayLedgerViewModel = viewModel(
        viewModelStoreOwner = app as ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    TodayLedgerScreen(viewModel = vm)
}

@Composable
private fun PlaceholderTab(label: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
