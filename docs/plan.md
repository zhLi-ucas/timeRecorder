# 柳比歇夫时间统计法 App — 实施计划

> 参考：`docs/gpt_plan.md`（产品定位与功能蓝图，但里面写的 Flutter/React Native 技术栈不适用本项目）
> 本计划把那份蓝图**落地到现有的 Android + Jetpack Compose 工程**，并明确每个阶段的可交付物与验收标准。
> 本文不实现任何代码，仅作为逐项细化与逐步实现的依据。

---

## 0. 与现有代码的关系

当前 `app/src/main/java/com/example/timemanager/` 下是一套**以"开始/结束一个 affair"为核心**的旧体系（`Task` / `TimeRecord` / `Tag` / `DurationRepository` / `HealthRecordRepository` / `AmbientDisplayActivity`），用户已明确弃用。

**结论：旧体系整体废弃，新体系按柳比歇夫法重建。** 复用的只有以下三块骨架：

| 复用 | 说明 |
|---|---|
| `TimeManagerApplication`（Application + ViewModelStoreOwner） | 应用级 ViewModel 作用域模式保留，新 ViewModel 挂在这里 |
| `MainActivity` 外壳 | 保留 Activity 入口；`Screen` 枚举与导航换成新的 |
| `ui/theme/` | Material3 配色字号保留 |
| Gradle 工程结构、`settings.gradle.kts` 的 Aliyun 镜像优先顺序、`local.properties` | 不动 |

**删除**：`TimerState`、`Task`、旧 `Tag`/`TagRepository`、`DurationRepository`、旧 `TimeRecord`/`TimeRecordRepository`、`HealthRecord`/`HealthRecordRepository`、`TimerViewModel`、`NotificationService`、`ReminderButton`、`ThermometerReminder`、`TagSelectionDialog`、`FinishTaskDialog`、`StartButton`、`HomeScreen`、`OptionsScreen`、`TimeRecordsScreen`、`AmbientDisplayActivity`、`AmbientDisplayScreen`。

新体系**不含实时计时器、不含健康提醒**——录入只走"分类+时长"单表单，时间线靠默认堆叠 + 拖拽。

删除动作放在 Phase 1。

---

## 1. 技术选型

| 维度 | 决定 | 理由 |
|---|---|---|
| UI | Jetpack Compose + Material3（已就位） | 项目现有栈 |
| 持久化 | **Room (SQLite)** | 旧 JSON/SharedPreferences 不适合按日期范围查询、聚合统计。Room 是 Android 一等公民 |
| DI | **不做**，`AppDatabase` 用 `Companion.getInstance(context)` 单例 | 符合 `CLAUDE.md` "No DI framework" 约定 |
| 导航 | 手写 `Screen` 枚举 + `rememberSaveable` + `BackHandler`（沿用现有模式） | 屏幕数固定为 5 |
| 图表 | MVP 用 Compose `Canvas` 手绘横向条形 + 百分比文字 | 不引第三方图表库 |
| 拖拽交互 | Compose `pointerInput { detectDragGesturesAfterLongPress }` + 手写 snap & cascade | 时间线 block 长按拖拽，5 分钟 snap |
| 序列化 | Room 实体 + `TypeConverter`（`LocalDate` ↔ epoch day；时间用 `Int` 分钟数，不需要转换） | 不再用 JSON 手写 |
| 异步 | `viewModelScope` + `Flow`（`StateFlow`/`SharedFlow`/`Channel`） | 与现有代码风格一致 |
| 字符串 | UI 文案、默认分类名一律中文（简体） | 符合 `CLAUDE.md` 约定 |

**需要新增的依赖**（写入 `gradle/libs.versions.toml`）：

- `androidx.room:room-runtime` / `room-ktx` / `room-compiler`（ksp） v2.6.1
- `androidx.room:room-testing`（测试）
- `com.google.devtools.ksp` gradle plugin v2.0.21-1.0.27
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`（如果没有）

---

## 2. 核心数据模型

### 2.1 实体定义

```text
TimeEntry（时间事件，最核心）
  id            String  (UUID)
  date          LocalDate        // 归属日期
  startMinOfDay Int              // 当天开始时刻，0–1439（分钟数）；endMinOfDay = startMinOfDay + durationMin 派生
  durationMin   Int              // 持续分钟数
  title         String           // 事件名称；留空时由二级分类名填充
  categoryId    String           // 二级分类 id（一级通过 parent 推导）
  projectId     String?          // 可选
  note          String?          // 备注（结果/产出）
  isEstimated   Boolean          // 是否为估算（保留字段，v1 录入不暴露）
  createdAt     LocalDateTime
  updatedAt     LocalDateTime

Category（两级时间账户）
  id          String
  name        String
  parentId    String?            // null = 一级，否则二级
  colorKey    String?            // 一级分类的颜色 key（参考 §2.2 配色表）；二级继承一级
  sortOrder   Int
  isArchived  Boolean
  isSystem    Boolean            // 系统内置（如"未分类"），不可删不可归档
  createdAt   LocalDateTime
  updatedAt   LocalDateTime

Project
  id          String
  name        String
  description String?
  isArchived  Boolean
  createdAt   LocalDateTime
  updatedAt   LocalDateTime

Review（周期复盘）
  id             String
  periodType     enum { DAY, WEEK, MONTH }   // v1 不含 YEAR
  periodStart    LocalDate
  periodEnd      LocalDate
  summaryText    String?
  mainFindings   String?
  adjustmentPlan String?
  createdAt      LocalDateTime
  updatedAt      LocalDateTime

AppSetting（key-value）
  key   String  PK
  value String
```

### 2.2 默认分类与配色（首次启动初始化）

直接采纳 `gpt_plan.md` 第九节方案。一级分类与 M3 配色：

| 一级 | colorKey |
|---|---|
| 核心工作 | blue |
| 辅助工作 | cyan |
| 学习研究 | green |
| 日常事务 | amber |
| 休息恢复 | orange |
| 社交沟通 | purple |
| 无效消耗 | grey |
| 未分类（系统） | neutral |

每个一级带 4–5 个二级。详细清单在 Phase 2 实现里给出。

### 2.3 关键算法

- **durationMin**：用户在表单里直接输入；不再由 start/end 派生
- **startMinOfDay**：
  - 新建 entry 时默认 = **当天已有 entry 的最大 endMinOfDay**；当天为空时 = **默认起点 480（08:00）**
  - 默认起点可在 AppSetting 配置（key: `day_start_min`）
- **block 拖拽**：
  - 长按 → 浮起 → 拖动 → 5 分钟 snap（startMinOfDay 向最近的 5 的倍数取整）
  - 落点与下方 block 重叠 → **下方 block 及其后所有 block 整体后移**（cascade shift）
  - 落点会让任一 block 越过 24:00 → **拒绝拖拽 + 回弹 + Toast** "今日已排满，请先调整已有条目"
  - 不允许跨天（date 字段不可变）
- **空白处理**：**不做空白段保留**。block 默认 back-to-back 紧贴；过渡时段由用户自己决定（要么显式建"午休""休息"等 entry，要么被前后段吸收）。统计页的"今日已记录时长"自然反映"今天记了几小时"，作为时间优化参考。
- **跨天**：禁止。拖拽超 24:00 拒绝；新建累计超 24:00 拒绝。
- **完整率**：**不做**。

---

## 3. 页面架构

五个一级页面，全部 Compose，挂在 `MainActivity` 里：

```text
Screen.TODAY      今日账本（首页，时间线 + 拖拽编辑）
Screen.RECORD     记一笔（单表单：日期 + 分类 + 时长 + 标题）
Screen.STATS      统计（范围 + 一/二级分类横向条形 + 项目汇总）
Screen.REVIEW     复盘（日/周/月模板）
Screen.SETTINGS   设置（分类/项目管理 + 导出/备份）
```

底部导航栏（Material3 `NavigationBar`）切换。废弃旧 `HOME/OPTIONS/RECORDS` 枚举与 HorizontalPager 主页结构。

### 各页面职责

| 页面 | 职责 | 主要交互 |
|---|---|---|
| 今日账本 | 日期切换 + 时间线（block 紧贴堆叠） + 当天总时长 + "+ 记一笔" | 长按拖拽 reorder（5 分钟 snap + cascade shift）；点击编辑；长按菜单删除 |
| 记一笔 | 单一录入表单 | 选日期（默认今天）→ 一级 → 二级 → 时长（chip + 自定义）→ 标题（可选）→ 项目/备注（可选）→ 保存。保存后追加到当天末尾 |
| 统计 | 范围 tab（今日/本周/本月/今年/自定义，默认本月） + 一级分类横向条形 + 二级展开 + 项目汇总 | 范围切换、点击展开二级 |
| 复盘 | 周期 tab（日/周/月） + 当前周期模板表单 + 历史复盘列表 | 新建、编辑、删除 |
| 设置 | 分类管理、项目管理、默认起点配置、导出 CSV/Markdown、JSON 备份/恢复 | CRUD + SAF 导出 |

### ViewModel 划分

- `TodayLedgerViewModel`（Application scoped）—— 当天 entries + 拖拽 reorder 状态
- `RecordViewModel` —— 表单状态
- `StatsViewModel` —— 范围 + 聚合结果
- `ReviewViewModel` —— 复盘 CRUD
- `SettingsViewModel` —— 分类/项目/导出/备份

所有 ViewModel 都从 `TimeManagerApplication` 这个 `ViewModelStoreOwner` 取，保持跨 Activity 重建存活（沿用 `CLAUDE.md` 强调的不变量）。

---

## 4. 阶段拆分（逐步实现）

每个阶段是**一个独立可交付单元**：完成 → 编译过 → 装到设备上能验证 → 再进下一阶段。逐项细化时，每阶段我会先把"这一步要改的文件清单 + 数据结构 + 验收点"列给你确认，再动手。

### Phase 1 — 清理与地基

**交付**：

- 删除第 0 节列出的所有旧类与旧屏幕
- `MainActivity` 改为只承载新 5 个 `Screen` 枚举的占位 Composable（每个屏幕先写空 `Box` + 标题）
- 底部 `NavigationBar` 接好 5 个 tab 的切换
- `gradle/libs.versions.toml` 增加 Room + KSP 依赖项
- `app/build.gradle.kts` 加 `ksp` plugin
- `TimeManagerApplication` 保留并准备 `AppDatabase.getInstance(this)`

**验收**：`./gradlew assembleDebug` 通过；装机后看到 5 个空 tab 可切换；旧代码全部从 git 历史里可找回，不在源码树里。

### Phase 2 — 数据层（Room + 默认数据）

**交付**：

- `data/entity/`：`TimeEntryEntity`、`CategoryEntity`、`ProjectEntity`、`ReviewEntity`、`AppSettingEntity`
- `data/converter/Converters.kt`：`LocalDate` ↔ epoch day；`LocalDateTime` ↔ epoch millis
- `data/db/AppDatabase.kt`：`@Database` + `@TypeConverters`，单例 `getInstance(context)`，`version = 1`
- `data/dao/`：`TimeEntryDao`、`CategoryDao`、`ProjectDao`、`ReviewDao`、`AppSettingDao`
  - `TimeEntryDao` 必须有：`insert/update/delete`、`getByDate(date)`、`getByDateRange(from, to)`、`sumDurationByCategory(from, to)`（`@Query` 聚合）、`getMaxEndMinOfDay(date)`
- `data/repo/`：每个 DAO 包一层 Repository，由 ViewModel 持有 `application` 构造
- `DefaultDataSeeder`：首次启动（AppSetting `seeded=false` 时）插入默认一/二级分类（含 colorKey）、默认 `day_start_min=480`、默认单位等

**验收**：

- JVM 单元测试（Room in-memory）覆盖 insert / query / sum / getMaxEnd
- 装机后 adb 检查 `/data/data/com.example.timemanager/databases/` 出现新库
- 默认分类能在设置页空壳里临时列出来（验证 seeder 跑过）

### Phase 3 — 今日账本（时间线 + 拖拽，最复杂）

**交付**：

- `ui/screens/TodayLedgerScreen.kt`：日期标题 + 当天总记录时长 + 时间线 + "+ 记一笔" FAB
- `ui/components/TimelineBlock.kt`：单个 entry block（按一级分类配色着色，显示时间区间 + 标题 + 时长）
- `ui/components/DayTimeline.kt`：时间线容器，承载所有 block；block 之间紧贴堆叠；处理长按 → 浮起 → 拖动 → snap → cascade shift
- `TodayLedgerViewModel`：暴露 `Flow<List<TimeEntry>>`；提供 `moveEntry(id, newStartMinOfDay)` 接口（内部处理 cascade + 越界拒绝）
- 拖拽触感反馈（轻振动）
- 点击 block → 弹出编辑弹窗（复用 Phase 4 的表单）或长按菜单 → 删除

**验收**：

- 当天没数据时显示空状态 + "+ 记一笔" CTA
- 手动塞两条 entry（如 480–540 阅读 / 540–600 写作），看到 block 紧贴从 08:00 起
- 长按"写作" block 拖到 700（11:40），落点会与"阅读"重叠 → cascade shift 把"阅读"推到 700–760（11:40–12:40），"写作"变 480–540
- 拖到会让任一 block 越过 24:00 → 拒绝 + Toast
- 点击 block 弹出编辑表单

### Phase 4 — 记一笔（单一表单）

**交付**：

- `ui/screens/RecordScreen.kt`：单表单，可由"今日账本 + 记一笔"或编辑现有 entry 触发
- `ui/components/CategoryPicker.kt`：两级分类选择（一级 chip 横排 + 二级列表）
- `ui/components/DurationInput.kt`：快捷 chip（15/30/60/90/120）+ 数字键盘自定义；最小 1 分钟，建议 snap 5
- `ui/components/ProjectPicker.kt`（可选字段）：项目选择弹窗
- `RecordViewModel`：表单状态；保存逻辑 = 构造 `TimeEntry` 写库（startMinOfDay 由 ViewModel 调 DAO 的 `getMaxEndMinOfDay(date)` 计算）；标题留空时用二级分类名填充
- 越界检查：`startMinOfDay + durationMin > 1440` → Toast 拒绝

**验收**：

- 30 秒内能填完一条记录（选日期 + 分类 + 时长 + 标题）
- 标题留空时列表显示二级分类名
- 当天累计超 24:00 时拒绝 + Toast
- 编辑现有 entry 时预填字段，保存后 startMinOfDay 不变（除非用户主动拖拽）

### Phase 5 — 统计页

**交付**：

- `ui/screens/StatsScreen.kt`：顶部范围 tab（今日/本周/本月/今年/自定义，默认本月） + 汇总区
- `ui/components/CategoryBar.kt`：单条分类的横向条形（按一级配色着色）+ 时长 + 百分比
- `StatsViewModel`：根据范围算 `from`/`to`（本周起始周一），调 DAO 聚合查询；二级分类按一级分组展开
- 年度视图：按月分桶，显示 12 个月柱状

**验收**：

- 任意范围切换都能看到一/二级分类的总量、占比
- 范围内无数据时显示空状态
- 项目汇总在用户有项目数据时显示

### Phase 6 — 复盘页

**交付**：

- `ui/screens/ReviewScreen.kt`：周期 tab（日/周/月） + 当前周期模板表单 + 历史复盘列表
- 模板字段（日）：最有价值的时间段 / 最大的浪费 / 明天调整
- 模板字段（周）：本周主要时间投入 / 时间结构问题 / 下周调整
- 模板字段（月）：本月核心工作时间 / 主要成果 / 时间偏差 / 下月时间预算
- `ReviewViewModel`：CRUD；保存时附带当时该周期的统计快照（在 summaryText 里自动插入小计）

**验收**：可创建、编辑、删除复盘；重启后仍在；同一周期可有多条复盘（按时间倒序）。

### Phase 7 — 设置页（分类/项目管理 + 导出）

**交付**：

- `ui/screens/SettingsScreen.kt`：分区入口
- `ui/components/CategoryManager.kt`：分类 CRUD（新增一级/二级、改名、归档、调色、排序）；系统分类（"未分类"）禁用所有操作；其他默认分类可归档不可删除
- `ui/components/ProjectManager.kt`：项目 CRUD
- `ui/components/DayStartPicker.kt`：默认起点配置（影响新建 entry 的落点）
- 导出：CSV（按 gpt_plan 字段）+ Markdown（按 gpt_plan 模板）→ SAF
- 备份：JSON 单文件 + `version: 1`，含 4 张表 + 设置；恢复从 JSON 读回

**验收**：归档分类不参与新建选择但保留历史 entry 的引用；CSV 能用 Excel/Numbers 打开；JSON 备份恢复后数据一致。

### Phase 8 — 打磨与回归

**交付**：

- 暗色主题适配（已有 M3 主题，主要检查对比度 + 一级配色在暗色下的可读性）
- 横屏布局（统计页 / 复盘页需要）
- 启动图标与名称（Phase 8 决定是否换掉 `d87fe26 feat: update a temp icon`）
- Lint 通过、`./gradlew test` 通过
- 更新 `README.md` 的"重大版本重构与实施规划"段落

**v1 落地决策**：

- 横屏改为**锁竖屏**（`screenOrientation=portrait`），不做横屏自适应
- 启动图标替换为 Claude 生成的简单矢量图标（白圆 + 3 横条 on 蓝）+ Android 13+ monochrome themed icon
- 暗色适配方案：`CategoryColors` 加 light/dark 双 palette（Material 200 tints），`@Composable colorFor()` 跟 `isSystemInDarkTheme()` 切换
- Lint 修复方案：启用 core library desugaring（`desugar_jdk_libs:2.1.5`）让 `java.time` 在 minSdk 24 上可用，一次性消化 94 个 NewApi 错误
- 清理 5 个 webp 前景（adaptive icon 现走 vector drawable）+ 7 个未引用 color（legacy purple/teal/black/white）

---

## 5. 明确不做的事（第一版）

1. 不做实时计时器（无 start/stop 按钮，无运行中状态）
2. 不做 todo list / 任务优先级
3. 不做番茄钟主流程
4. 不做积分、连续打卡、成就系统
5. 不做 AI 总结
6. 不做社交分享 / 团队协作
7. 不做情绪追踪
8. 不做健康数据接入（旧喝水/起立提醒删除）
9. 不做云同步
10. 不做完整率指标
11. 不做空白段保留（block 默认紧贴堆叠）
12. 不做跨天 entry
13. 不做年度复盘（v1 只做日/周/月）
14. 不做历史搜索

---

## 6. 已确认决策（Phase 0 完成）

### 6.1 旧代码处理
- AmbientDisplayActivity：删除（息屏交给系统）
- 健康提醒（喝水/起立）：删除（交给手机其他功能）
- 旧 TimeRecord JSON：不迁移，直接清空

### 6.2 数据 / 存储
- 跨天 entry：禁止
- 时区：本地时区（`LocalDate` 存日期，`Int` 存当天分钟数，无时区转换问题）
- Room 2.6.1 + KSP 2.0.21-1.0.27
- 默认起点：08:00（可在设置改）
- 拖拽 snap：5 分钟
- 拖拽重叠：cascade shift（下方 block 整体后移）
- 越界（> 24:00）：拒绝 + Toast

### 6.3 输入 / 展示
- 时长输入：快捷 chip（15/30/60/90/120）+ 数字键盘自定义
- 一级分类配色：blue / cyan / green / amber / orange / purple / grey / neutral（详见 §2.2）
- 时间显示：24 小时制
- 标题：可选；留空用二级分类名填充
- 主题：跟随系统（亮/暗）

### 6.4 范围 / 周期
- 每周起始：周一
- 统计页默认范围：本月
- 复盘周期：日 / 周 / 月（年度留 v2）

### 6.5 分类管理
- 默认 8 个一级分类：可归档、不可删除
- "未分类"：系统分类，不可归档、不可删除
- 自定义分类：可增删改、可归档

### 6.6 导出 / 备份
- CSV：按 gpt_plan 字段
- Markdown：按 gpt_plan 模板
- JSON 备份：单文件 + `version: 1`，含 4 张表 + 设置
- 落点：SAF 让用户选目录

### 6.7 其他（自行决定）
- 拖拽 snap 时轻触感反馈
- 历史搜索：v1 不做
- 应用图标：Phase 8 决定
- 首次打开：空时间线 + 一行提示

---

## 7. 进度跟踪

```text
[x] Phase 0  决策对齐（§6 已确认）
[x] Phase 1  清理与地基
[x] Phase 2  数据层（Room + 默认数据）
[x] Phase 3  今日账本（时间线 + 拖拽）
[x] Phase 4  记一笔（单一录入表单）
[x] Phase 5  统计页
[x] Phase 6  复盘页（日/周/月）
[x] Phase 7  设置页（分类/项目/导出）
[x] Phase 8  打磨与回归
```
