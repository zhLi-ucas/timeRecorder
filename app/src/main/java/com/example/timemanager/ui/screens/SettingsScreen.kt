package com.example.timemanager.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.timemanager.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class SettingsSubpage { ROOT, CATEGORIES, PROJECTS, AI_DEEPSEEK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (SettingsSubpage) -> Unit
) {
    val dayStart by viewModel.dayStartMin.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dayStartInput by remember(dayStart) { mutableStateOf(dayStart.toString()) }

    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val csv = viewModel.buildCsv()
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(csv.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "CSV 已导出", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val mdLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val md = viewModel.buildMarkdown()
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(md.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Markdown 已导出", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val jsonBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.buildJsonBackup()
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "JSON 备份已导出", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: return@launch
                val count = viewModel.restoreFromJson(json)
                Toast.makeText(context, "已恢复 $count 条记录", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("数据管理")
            NavRow("分类管理") { onNavigate(SettingsSubpage.CATEGORIES) }
            NavRow("项目管理") { onNavigate(SettingsSubpage.PROJECTS) }
            NavRow("AI / DeepSeek") { onNavigate(SettingsSubpage.AI_DEEPSEEK) }
            HorizontalDivider()

            SectionHeader("默认起点")
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "新建 entry 时若当天为空，从该时刻开始堆叠（0-1439 分钟）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dayStartInput,
                        onValueChange = { text ->
                            dayStartInput = text.filter { it.isDigit() }
                        },
                        label = { Text("分钟（如 480 = 08:00）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            dayStartInput.toIntOrNull()?.let { viewModel.setDayStartMin(it) }
                        }
                    ) { Text("保存") }
                }
            }
            HorizontalDivider()

            SectionHeader("导出")
            NavRow("导出 CSV") {
                csvLauncher.launch("timemanager-$today.csv")
            }
            NavRow("导出 Markdown") {
                mdLauncher.launch("timemanager-$today.md")
            }
            HorizontalDivider()

            SectionHeader("备份与恢复")
            NavRow("导出 JSON 备份") {
                jsonBackupLauncher.launch("timemanager-backup-$today.json")
            }
            NavRow("从 JSON 恢复") {
                jsonRestoreLauncher.launch(arrayOf("application/json"))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(">", style = MaterialTheme.typography.titleLarge)
    }
}
