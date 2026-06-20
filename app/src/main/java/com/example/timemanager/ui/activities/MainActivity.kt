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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.timemanager.ui.screens.CategoryManagerScreen
import com.example.timemanager.ui.screens.ProjectManagerScreen
import com.example.timemanager.ui.screens.RecordScreen
import com.example.timemanager.ui.screens.ReviewScreen
import com.example.timemanager.ui.screens.SettingsScreen
import com.example.timemanager.ui.screens.SettingsSubpage
import com.example.timemanager.ui.screens.StatsScreen
import com.example.timemanager.ui.screens.TodayLedgerScreen
import com.example.timemanager.ui.theme.TimeManagerTheme
import com.example.timemanager.viewmodel.RecordViewModel
import com.example.timemanager.viewmodel.ReviewViewModel
import com.example.timemanager.viewmodel.SettingsViewModel
import com.example.timemanager.viewmodel.StatsViewModel
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as Application
    var currentScreen by rememberSaveable { mutableStateOf(Screen.TODAY) }

    val todayVm: TodayLedgerViewModel = viewModel(
        viewModelStoreOwner = app as ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    val recordVm: RecordViewModel = viewModel(
        viewModelStoreOwner = app as ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    val statsVm: StatsViewModel = viewModel(
        viewModelStoreOwner = app as ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    val reviewVm: ReviewViewModel = viewModel(
        viewModelStoreOwner = app as ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    val settingsVm: SettingsViewModel = viewModel(
        viewModelStoreOwner = app as ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    var settingsSubpage by rememberSaveable { mutableStateOf(SettingsSubpage.ROOT) }

    val showBottomBar = currentScreen != Screen.RECORD

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentScreen) {
                Screen.TODAY -> TodayLedgerScreen(
                    viewModel = todayVm,
                    onRecordClick = {
                        recordVm.startNew()
                        currentScreen = Screen.RECORD
                    },
                    onEditEntry = { id ->
                        recordVm.loadForEdit(id)
                        currentScreen = Screen.RECORD
                    }
                )
                Screen.RECORD -> RecordScreen(
                    viewModel = recordVm,
                    onDone = { currentScreen = Screen.TODAY },
                    onCancel = { currentScreen = Screen.TODAY }
                )
                Screen.STATS -> StatsScreen(viewModel = statsVm)
                Screen.REVIEW -> ReviewScreen(viewModel = reviewVm)
                Screen.SETTINGS -> when (settingsSubpage) {
                    SettingsSubpage.ROOT -> SettingsScreen(
                        viewModel = settingsVm,
                        onNavigate = { settingsSubpage = it }
                    )
                    SettingsSubpage.CATEGORIES -> CategoryManagerScreen(
                        viewModel = settingsVm,
                        onBack = { settingsSubpage = SettingsSubpage.ROOT }
                    )
                    SettingsSubpage.PROJECTS -> ProjectManagerScreen(
                        viewModel = settingsVm,
                        onBack = { settingsSubpage = SettingsSubpage.ROOT }
                    )
                }
            }
        }
    }
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
