package com.example.timemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.timemanager.data.DefaultDataSeeder
import com.example.timemanager.viewmodel.DeepSeekTestState
import com.example.timemanager.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDeepSeekScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val apiKey by viewModel.deepseekApiKey.collectAsState()
    val model by viewModel.deepseekModel.collectAsState()
    val testState by viewModel.deepseekTestState.collectAsState()

    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    val keyDirty = keyInput != apiKey

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI / DeepSeek") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "API Key",
                style = MaterialTheme.typography.titleSmall
            )
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("DeepSeek API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.setDeepSeekApiKey(keyInput.trim()) },
                enabled = keyDirty && keyInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存 API Key") }

            HorizontalDivider()

            Text(
                text = "模型",
                style = MaterialTheme.typography.titleSmall
            )
            ModelRadioRow(
                label = "deepseek-v4-flash",
                description = "默认 · 快 · 便宜",
                selected = model == DefaultDataSeeder.DEFAULT_DEEPSEEK_MODEL,
                onSelect = { viewModel.setDeepSeekModel(DefaultDataSeeder.DEFAULT_DEEPSEEK_MODEL) }
            )
            ModelRadioRow(
                label = "deepseek-v4-pro",
                description = "推理强 · 慢 · 贵",
                selected = model == "deepseek-v4-pro",
                onSelect = { viewModel.setDeepSeekModel("deepseek-v4-pro") }
            )

            HorizontalDivider()

            Button(
                onClick = { viewModel.testDeepSeekConnection() },
                enabled = apiKey.isNotBlank() && testState !is DeepSeekTestState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (testState is DeepSeekTestState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("测试连接")
                }
            }
            when (val s = testState) {
                is DeepSeekTestState.Success -> Text(
                    text = "上次测试：成功（${s.latencyMs} ms）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                is DeepSeekTestState.Error -> Text(
                    text = "失败 — ${s.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                else -> Unit
            }

            HorizontalDivider()

            Text(
                text = "隐私说明",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "使用 AI 生成复盘时，你本日 / 本周 / 本月的时间记录（含分类、时长、备注、效能）" +
                    "会被发送到 DeepSeek 服务器处理。API key 明文存储于本机数据库，" +
                    "请在了解这一点后再启用。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModelRadioRow(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
