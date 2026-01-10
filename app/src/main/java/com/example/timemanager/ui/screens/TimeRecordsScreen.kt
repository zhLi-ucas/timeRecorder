package com.example.timemanager.ui.screens

import android.app.Application
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.data.Tag
import com.example.timemanager.data.TimeRecord
import com.example.timemanager.viewmodel.TimerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimeRecordsScreen(
    viewModel: TimerViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val actualViewModel: TimerViewModel = viewModel ?: viewModel(
        viewModelStoreOwner = context.applicationContext as androidx.lifecycle.ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )

    val records by actualViewModel.records.collectAsState()
    val tags by actualViewModel.tags.collectAsState()
    
    // State for date navigation using Pager
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
    val scope = rememberCoroutineScope()
    
    // Base date for calculating page dates (Today)
    val baseDate = remember { Calendar.getInstance() }
    
    // Calculate current date from pager state
    val currentDate = remember(pagerState.currentPage) {
        val date = baseDate.clone() as Calendar
        date.add(Calendar.DAY_OF_YEAR, pagerState.currentPage - initialPage)
        date
    }
    
    // Helper function to calculate date for any page
    fun getDateForPage(page: Int): Calendar {
        val date = baseDate.clone() as Calendar
        date.add(Calendar.DAY_OF_YEAR, page - initialPage)
        return date
    }

    // Date navigation helper functions
    fun previousDay() {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    fun nextDay() {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    fun showDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)
                
                // Calculate page difference
                val diffInMillis = selectedDate.timeInMillis - baseDate.timeInMillis
                val diffDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
                
                // Adjust for potential timezone/DST issues with simple division
                // A more robust way:
                val tempBase = baseDate.clone() as Calendar
                // We need to find exact day difference. 
                // Since this is UI logic, we can iterate or use library if needed.
                // Simple approximation is usually fine if we stick to noon or handle it carefully.
                // Let's use a loop for small diffs or exact calculation.
                
                // Better approach: use java.time if possible, but sticking to Calendar:
                // We'll trust the simple diff for now, but to be safe, let's recalculate target page date
                val targetPage = initialPage + diffDays
                
                // Verify close pages to handle day boundary issues (e.g. 23h vs 25h days)
                var bestPage = targetPage
                var minDiff = Long.MAX_VALUE
                
                // Search around the estimated page to find the exact date match
                for (offset in -1..1) {
                    val p = targetPage + offset
                    val d = getDateForPage(p)
                    if (isSameDay(d, selectedDate)) {
                        bestPage = p
                        break
                    }
                }
                
                scope.launch {
                    pagerState.scrollToPage(bestPage)
                }
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Create a map for quick tag color lookup
    val tagColorMap = remember(tags) { 
        tags.associate { it.name to Color(it.colorArgb) } 
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<TimeRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingRecord = null
                            showEditDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加记录")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Date Navigation Bar
            val isToday = isSameDay(currentDate, Calendar.getInstance())
            val navBarColor = if (isToday) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant

            Surface(
                color = navBarColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { previousDay() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "前一天")
                    }
                    
                    Row(
                        modifier = Modifier
                            .clickable { showDatePicker() }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DateRange, 
                            contentDescription = "选择日期",
                            modifier = Modifier.size(20.dp),
                            tint = if (isToday) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatDateHeader(currentDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = { nextDay() }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "后一天")
                    }
                }
            }

            // Records List with Pager Support
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageDate = getDateForPage(page)
                
                // Filter records for the page's date
                val pageRecords = remember(records, page) {
                    records.filter { record ->
                        val recordCal = Calendar.getInstance().apply { timeInMillis = record.startTime }
                        isSameDay(pageDate, recordCal)
                    }.sortedBy { it.startTime } // Sorted Ascending (Morning to Night)
                }

                if (pageRecords.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pageRecords) { record ->
                            val tagColor = tagColorMap[record.tag] ?: MaterialTheme.colorScheme.surfaceVariant
                            TimeRecordItem(
                                record = record,
                                tagColor = tagColor,
                                onClick = {
                                    editingRecord = record
                                    showEditDialog = true
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditTimeRecordDialog(
            record = editingRecord,
            allTags = tags,
            initialDate = currentDate.timeInMillis, // Pass current selected date
            onDismiss = { showEditDialog = false },
            onConfirm = { record ->
                if (editingRecord == null) {
                    actualViewModel.addRecord(record)
                } else {
                    actualViewModel.updateRecord(record)
                }
                showEditDialog = false
            },
            onDelete = { record ->
                actualViewModel.deleteRecord(record.id)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun TimeRecordItem(
    record: TimeRecord,
    tagColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tagColor.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = record.tag,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatDurationDisplay(record.durationSeconds),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (record.description.isNotBlank()) {
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Text(
                text = formatTimeRange(record.startTime, record.endTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun EditTimeRecordDialog(
    record: TimeRecord?,
    allTags: List<Tag>,
    initialDate: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onConfirm: (TimeRecord) -> Unit,
    onDelete: (TimeRecord) -> Unit
) {
    // Initial State Initialization
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    // Initialize with record data or current time
    val initStartTime = record?.startTime ?: initialDate
    val initEndTime = record?.endTime ?: initialDate
    
    // Parse initial values
    val startCal = Calendar.getInstance().apply { timeInMillis = initStartTime }
    val endCal = Calendar.getInstance().apply { timeInMillis = initEndTime }
    
    // State variables
    var selectedDate by remember { mutableStateOf(startCal.timeInMillis) }
    var isNextDay by remember { 
        mutableStateOf(
            endCal.get(Calendar.YEAR) > startCal.get(Calendar.YEAR) || 
            endCal.get(Calendar.DAY_OF_YEAR) > startCal.get(Calendar.DAY_OF_YEAR)
        )
    }
    
    var startHour by remember { mutableStateOf(startCal.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableStateOf(startCal.get(Calendar.MINUTE)) }
    
    var endHour by remember { mutableStateOf(endCal.get(Calendar.HOUR_OF_DAY)) }
    var endMinute by remember { mutableStateOf(endCal.get(Calendar.MINUTE)) }
    
    var selectedTag by remember { mutableStateOf(record?.tag ?: if (allTags.isNotEmpty()) allTags[0].name else "Default") }
    var description by remember { mutableStateOf(record?.description ?: "") }

    // Helper functions for Dialogs
    fun showDatePicker() {
        calendar.timeInMillis = selectedDate
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker(isStart: Boolean) {
        val initialHour = if (isStart) startHour else endHour
        val initialMinute = if (isStart) startMinute else endMinute
        
        TimePickerDialog(
            context,
            { _, hour, minute ->
                if (isStart) {
                    startHour = hour
                    startMinute = minute
                } else {
                    endHour = hour
                    endMinute = minute
                }
            },
            initialHour,
            initialMinute,
            true
        ).show()
    }

    // Calculate final timestamps
    val finalStartTime = remember(selectedDate, startHour, startMinute) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val finalEndTime = remember(selectedDate, isNextDay, endHour, endMinute) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDate
            if (isNextDay) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    val isValid = if (!isNextDay) finalEndTime >= finalStartTime else true
    val durationSeconds = (finalEndTime - finalStartTime) / 1000

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record == null) "添加记录" else "编辑记录") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tag Selection
                Text("标签", style = MaterialTheme.typography.labelLarge)
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    items(allTags) { tag ->
                        FilterChip(
                            selected = tag.name == selectedTag,
                            onClick = { selectedTag = tag.name },
                            label = { Text(tag.name) }
                        )
                    }
                    if (allTags.isEmpty()) {
                        item {
                            Text("无可用标签，请在设置中添加", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Date Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("日期", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { showDatePicker() }) {
                            Text(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDate)))
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("跨日(+1天)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isNextDay,
                            onCheckedChange = { isNextDay = it }
                        )
                    }
                }

                // Time Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("开始时间", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { showTimePicker(true) }) {
                            Text(String.format("%02d:%02d", startHour, startMinute))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("结束时间", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { showTimePicker(false) }) {
                            Text(
                                text = String.format("%02d:%02d", endHour, endMinute),
                                color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        if (!isValid) {
                            Text("结束时间不能早于开始时间", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Duration Display
                Text(
                    text = "持续时间: ${formatDuration(durationSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 50) description = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${description.length}/50") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (durationSeconds >= 0) {
                        onConfirm(
                            TimeRecord(
                                id = record?.id ?: UUID.randomUUID().toString(),
                                tag = selectedTag,
                                startTime = finalStartTime,
                                endTime = finalEndTime,
                                durationSeconds = durationSeconds,
                                description = description
                            )
                        )
                    }
                },
                enabled = selectedTag.isNotBlank() && isValid
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                if (record != null) {
                    TextButton(
                        onClick = { onDelete(record) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("删除")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

private fun formatDateHeader(date: Calendar): String {
    val today = Calendar.getInstance()
    
    return when {
        isSameDay(today, date) -> "今天"
        isYesterday(today, date) -> "昨天"
        else -> SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(date.time)
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(today: Calendar, target: Calendar): Boolean {
    val yesterday = today.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(yesterday, target)
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return String.format("%02d:%02d", h, m)
}

private fun formatDurationDisplay(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) {
        "$h h $m min"
    } else {
        "$m min"
    }
}

private fun formatTimeRange(start: Long, end: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
}
