# 2026-06-21 16:20 TODAY 历史浏览与编辑 session

> 时间戳：**2026-06-21 16:20**（本地，Redmi 2407FRK8EC 实测机时区）

## 目标

TODAY tab 此前虽 VM 内部已用 `_selectedDate: MutableStateFlow<LocalDate>` 查询（`TodayLedgerViewModel.kt:50`），但没公开 setter、UI 也只展示"今天"，导致用户想看/改昨天的账本只能去 STATS 翻汇总。本次让 TODAY 变成可回溯的每日账本：

- 手指左/右滑切换日期（**标准方向**：右→左=未来、左→右=过去；**未来完全禁止**，最新到当天）
- 头部加日历入口（点日期文字 + 右侧 `CalendarMonth` 图标，**两者都要**）
- 选定日期后，编辑（长按拖拽 reorder、+FAB 录入、点条目编辑）和当天**完全一致**
- 头部标题：今天显示"今日账本"，其他天显示日期

## 范围

| § | 内容 | Commit |
|---|---|---|
| §1 | `TodayLedgerViewModel` 暴露 `setDate` / `shiftDays`（钳制到当天），toast 文案去"今日"硬编码 | `2dbfed0` |
| §2 | `RecordViewModel.startNew(initialDate)` 让 TODAY FAB 可带过去日进 RECORD | `2dbfed0` |
| §3 | `MainActivity` TODAY 的 `onRecordClick` 传 `todayVm.selectedDate.value` | `2dbfed0` |
| §4 | `TodayLedgerScreen` 包 `HorizontalPager` + `TodayHeader` 改造 + 日历 Dialog + 标题/空态文案 | `2dbfed0` |
| §5 | `RecordScreen` DateRow 的 `DatePicker` 同样禁未来，与 TODAY 一致 | `2dbfed0` |
| docs | 本 session log | `<this>` |

整期一个 commit，因为 5 个文件强耦合、单独提没意义。

## 关键技术决策

### 1. `HorizontalPager` + 双向同步，VM 始终是真相源

pager 用 `pageCount = Int.MAX_VALUE`，`PIVOT = 100_000` 映射今天，过去页 = PIVOT - N。两个 `LaunchedEffect` 做双向同步：

- **pager → VM**：`snapshotFlow { pagerState.currentPage }` 监听翻页；若 `pageToDate(page)` 跑到未来就 `scrollToPage` 弹回 PIVOT，否则 `vm.setDate(date)`。用 `currentPage` 而非 `settledPage`，VM 在翻页动画中途就更新，体感更跟手。
- **VM → pager**：`snapshotFlow { vm.selectedDate.value }` 监听；若 `dateToPage(date) != currentPage` 就 `animateScrollToPage`。比较 `currentPage` 而非 `settledPage`，避免 swipe 触发的 VM 更新又反触发动画形成自激。

两个 flow 都 `distinctUntilChanged()`，自激循环在等值判断处自然终止。

### 2. 单页内容只在 `pageDate == selectedDate` 时渲染

每个 page composable 不持自己的数据，全靠 VM 的 `uiState.entries`（已 `flatMapLatest(_selectedDate)` 自动重查）。但 swipe 过程中两个 page 同时可见，如果都渲染 `uiState.entries` 会出现"两页内容一样滑过去"的违和感。解决：每页用 `if (pageDate == selectedDate)` 门控 —— 只有与 VM 当前日期匹配的那一页渲染内容，其他渲染 `Spacer`。

视觉效果：swipe 到一半时 currentPage 翻转 → VM 更新 → 内容"转移"到新页。可接受。

`beyondViewportPageCount` 默认 0，settle 后只有当前页 composed。

### 3. `setDate` 钳制到 `LocalDate.now()`

VM 和 RecordVM 的 setter 都在内部 `min(date, today)`。这意味着 UI 即使传了未来日（比如日历组件 bug）也安全。三层防御：

1. `DatePicker` 的 `selectableDates` 让未来日不可选
2. VM 的 `setDate` 钳制
3. pager 的 `currentPage` 监听里再次 `scrollToPage(PIVOT)` 弹回

### 4. API 教训：`selectableDates` 在 `rememberDatePickerState` 上，不在 `DatePicker` 上

第一次编译挂了：

```
e: RecordScreen.kt:193:17 No parameter with name 'selectableDates' found.
```

BOM `2024.09.00` 对应 Material3 1.3.0，签名是：

```kotlin
DatePicker(state, modifier, dateFormatter, dateValidator, ...)   // ← dateValidator 这里
rememberDatePickerState(initialSelectedDateMillis, ..., selectableDates)  // ← selectableDates 这里
```

`SelectableDates` 接口（`isSelectableDate(utcTimeMillis: Long): Boolean`）走 `rememberDatePickerState` 的参数。`DatePicker` 自己只有 `dateValidator: (Long) -> Boolean` lambda 形式。本次统一走 `selectableDates`，因为接口形式更易复用。

UTC 钳制：`utcTimeMillis <= LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()`。

### 5. VM 是 Application scope，日期跨 tab 自动保留

`MainActivity.AppContent` 把所有 VM 都挂在 `application as ViewModelStoreOwner`（`MainActivity.kt:83-86`），切 STATS 再回 TODAY 不会丢 `_selectedDate`。不需要 `rememberSaveable`。pager 重建时 `initialPage = dateToPage(selectedDate, today)` 自然回到正确页。

### 6. toast 文案去"今日"

`TodayLedgerViewModel` 第 90 / 116 行的 `"今日已排满"` → `"该日已排满"`；`RecordViewModel` 第 118 行的 `"今日已排满，无法添加"` → `"该日已排满，无法添加"`。同样适用于历史日编辑场景。

## 当前状态（feature 完工）

- TODAY 屏：头部标题（今日/历史自适应） + 可点日期文字 + 右侧日历图标；下方 HorizontalPager 承载 DayTimeline
- 滑动：左→右过去、右→左被钳制到今天
- 日历：DatePickerDialog 未来日灰选
- 过去日编辑：长按 reorder、+FAB（预填该日）、点条目编辑 —— 全部和当天一致
- RECORD 屏：DateRow 的 DatePicker 同样禁未来
- 日期跨 tab 切换保留

## 已知遗留

- **跨午夜 stale "today"**：`LocalDate.now()` 在 `TodayLedgerScreen` 用 `remember { ... }` 缓存，app 长期挂起过夜后仍是启动当天。pager 也会因此把"真正的今天"当成过去日，钳制逻辑失效。v1 不修，留待后续。
- 日历**不显示有记录的日期标记**（用户本次没要求）。如果要加，可走 `entryDao.observeByDateRange(monthStart, monthEnd)` 渲染小圆点。
- 未来日（包括今天 23:59 之后）完全禁止 —— 后续若要支持预约/计划，需要解锁未来并加新的录入约束。

## Commits

```
2dbfed0 feat(today): history browse + edit via swipe / date picker
<this>  docs: 2026-06-21 TODAY history browse session log
```

## 验证结果

- `./gradlew :app:installDebug`：通过
- 装机启动：`topResumedActivity = MainActivity`，logcat 无崩溃
- 手工 golden path（左滑过去、点日历跳转、过去日 reorder、过去日 FAB 录入）需要用户在设备上跑确认 —— 截图 CDN 缓存相同路径，Claude 侧目测不可靠（CLAUDE.md 已说明）
