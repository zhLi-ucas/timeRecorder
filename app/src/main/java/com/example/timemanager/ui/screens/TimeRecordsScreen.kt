package com.example.timemanager.ui.screens

import android.app.Application
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.data.Tag
import com.example.timemanager.data.TimeRecord
import com.example.timemanager.viewmodel.TimerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
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
    
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
    val scope = rememberCoroutineScope()
    
    val baseDate = remember { Calendar.getInstance() }
    val currentDate = remember(pagerState.currentPage) {
        val date = baseDate.clone() as Calendar
        date.add(Calendar.DAY_OF_YEAR, pagerState.currentPage - initialPage)
        date
    }
    
    fun getDateForPage(page: Int): Calendar {
        val date = baseDate.clone() as Calendar
        date.add(Calendar.DAY_OF_YEAR, page - initialPage)
        return date
    }

    fun previousDay() {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    fun nextDay() {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    fun showDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)
                val diffInMillis = selectedDate.timeInMillis - baseDate.timeInMillis
                val diffDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
                val targetPage = initialPage + diffDays
                
                var bestPage = targetPage
                for (offset in -1..1) {
                    val p = targetPage + offset
                    val d = getDateForPage(p)
                    if (isSameDay(d, selectedDate)) {
                        bestPage = p
                        break
                    }
                }
                scope.launch { pagerState.scrollToPage(bestPage) }
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val tagColorMap = remember(tags) { 
        tags.associate { it.name to Color(it.colorArgb) } 
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<TimeRecord?>(null) }
    var newRecordStart by remember { mutableStateOf(0L) }
    var newRecordEnd by remember { mutableStateOf(0L) }

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
                            newRecordStart = currentDate.timeInMillis
                            newRecordEnd = currentDate.timeInMillis + 30 * 60 * 1000
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

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageDate = getDateForPage(page)
                val pageRecords = remember(records, page) {
                    records.filter { record ->
                        val recordCal = Calendar.getInstance().apply { timeInMillis = record.startTime }
                        isSameDay(pageDate, recordCal)
                    }.sortedBy { it.startTime }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    TimelineView(
                        records = pageRecords,
                        tagColorMap = tagColorMap,
                        pageDate = pageDate,
                        onRecordClick = { record ->
                            editingRecord = record
                            showEditDialog = true
                        },
                        onEmptySpaceClick = { timeInMillis ->
                            val freeSlot = actualViewModel.findFreeTimeSlot(timeInMillis)
                            editingRecord = null
                            if (freeSlot != null) {
                                newRecordStart = freeSlot.first
                                newRecordEnd = freeSlot.second
                            } else {
                                newRecordStart = timeInMillis
                                newRecordEnd = timeInMillis + 30 * 60 * 1000
                            }
                            showEditDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditTimeRecordDialog(
            record = editingRecord,
            allTags = tags,
            initialStartTime = if (editingRecord != null) editingRecord!!.startTime else newRecordStart,
            initialEndTime = if (editingRecord != null) editingRecord!!.endTime else newRecordEnd,
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
fun TimelineView(
    records: List<TimeRecord>,
    tagColorMap: Map<String, Color>,
    pageDate: Calendar,
    onRecordClick: (TimeRecord) -> Unit,
    onEmptySpaceClick: (Long) -> Unit
) {
    val scrollState = rememberScrollState()
    val hourHeight = 60.dp 
    val totalHeight = hourHeight * 24
    val textMeasurer = rememberTextMeasurer()
    val rulerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(modifier = Modifier.height(totalHeight).fillMaxWidth()) {
            // Ruler
            Canvas(modifier = Modifier.width(50.dp).fillMaxHeight()) {
                val stepHeight = size.height / 24
                for (i in 0..24) {
                    val y = i * stepHeight
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    if (i < 24) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = String.format("%02d:00", i),
                            topLeft = Offset(8.dp.toPx(), y + 4.dp.toPx()),
                            style = TextStyle(fontSize = 12.sp, color = rulerColor)
                        )
                    }
                }
            }
            
            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(pageDate) {
                        detectTapGestures { offset ->
                            val totalHeightPx = size.height
                            val ratio = offset.y / totalHeightPx
                            val millisInDay = (ratio * 24 * 60 * 60 * 1000).toLong()
                            
                            val clickedTime = pageDate.clone() as Calendar
                            clickedTime.set(Calendar.HOUR_OF_DAY, 0)
                            clickedTime.set(Calendar.MINUTE, 0)
                            clickedTime.set(Calendar.SECOND, 0)
                            clickedTime.set(Calendar.MILLISECOND, 0)
                            clickedTime.add(Calendar.MILLISECOND, millisInDay.toInt())
                            
                            onEmptySpaceClick(clickedTime.timeInMillis)
                        }
                    }
            ) {
                 Canvas(modifier = Modifier.fillMaxSize()) {
                    val stepHeight = size.height / 24
                    for (i in 0..24) {
                        val y = i * stepHeight
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                 }
                 
                 records.forEach { record ->
                     val cal = Calendar.getInstance().apply { timeInMillis = record.startTime }
                     val startHour = cal.get(Calendar.HOUR_OF_DAY)
                     val startMin = cal.get(Calendar.MINUTE)
                     val durationMins = record.durationSeconds / 60f
                     
                     val startOffset = (startHour + startMin / 60f) * hourHeight.value
                     val height = (durationMins / 60f) * hourHeight.value
                     
                     val color = tagColorMap[record.tag] ?: MaterialTheme.colorScheme.primary
                     
                     Box(
                         modifier = Modifier
                             .padding(start = 2.dp, end = 8.dp)
                             .offset(y = startOffset.dp)
                             .height(height.dp.coerceAtLeast(20.dp))
                             .fillMaxWidth()
                             .background(color.copy(alpha = 0.8f), shape = MaterialTheme.shapes.small)
                             .clickable { onRecordClick(record) }
                             .padding(4.dp)
                     ) {
                         Text(
                             text = "${record.tag} ${if(record.description.isNotEmpty()) "- ${record.description}" else ""}",
                             style = MaterialTheme.typography.labelSmall,
                             color = Color.White,
                             maxLines = 1
                         )
                     }
                 }
            }
        }
    }
}

@Composable
fun EditTimeRecordDialog(
    record: TimeRecord?,
    allTags: List<Tag>,
    initialStartTime: Long,
    initialEndTime: Long,
    onDismiss: () -> Unit,
    onConfirm: (TimeRecord) -> Unit,
    onDelete: (TimeRecord) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    val startCal = Calendar.getInstance().apply { timeInMillis = initialStartTime }
    val endCal = Calendar.getInstance().apply { timeInMillis = initialEndTime }
    
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
                }

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
                    }
                }

                Text(
                    text = "持续时间: ${formatDuration(durationSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

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
