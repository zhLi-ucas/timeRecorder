# TimeManager

基于**柳比歇夫时间统计法**的 Android 时间账本 App（Jetpack Compose + Material3 + Room）。

录入只填**分类 + 时长**，时间线靠默认堆叠（紧贴 back-to-back）+ 长按拖拽 reorder；不做实时计时器、不做健康提醒、不做 todo —— 只做时间账本 + 分类统计 + 周期复盘 + 数据导出。

## 核心特性

- **Duration-first 录入**：日期 → 一级/二级分类（横向 ChoiceWheel 滑选）→ 时长（0–240min 5min 步进滑选）→ 效能（0–100 slider）→ 备注（可选）→ 保存，标题自动派生 `L1·L2`
- **今日时间线**：block 默认从默认起点（08:00）紧贴堆叠；点击编辑、长按菜单删除；**尾部「+」占位框**一键插 10min 休息/间隔
- **长按拖拽 reorder**：5 分钟 snap + cascade shift（重叠时下方 block 整体后移）；越过 24:00 拒绝
- **统计聚合**：日/周/月/年 4 范围（默认本周）；一/二级分类横向条形 + 百分比；**效能加权**（无效分钟计入 cat_invalid）
- **AI 复盘草稿**：REVIEW 一键调用 DeepSeek（v4-flash / v4-pro），上传双周期时间记录 + 上次复盘 → AI 生成三段文字（最有价值时段 / 浪费 / 调整），用户审阅微调后保存
- **周期复盘**：日 / 周 / 月三套模板字段，历史可回溯
- **分类 / 项目管理**：5 默认一级 + 15 二级，色块选择器调色，可归档、可新增；项目作为可选统计维度
- **导出 / 备份**：CSV / Markdown 报表 + JSON 全量备份与恢复
- **暗色主题**：跟随系统，分类色板 light/dark 双色（v1.2 翻转：亮模式用 saturated 深色，暗模式用浅色）
- **持久化**：Room v3 (SQLite)，App 重启 / Activity 重建均不丢状态；Room Migration 显式管理

## 重大版本重构与实施规划

柳比歇夫法 v1 已完成全部 8 个阶段（详见 `docs/plan.md`）：

- [x] Phase 0 — 决策对齐
- [x] Phase 1 — 清理旧体系（删除 Task / TimeRecord / Tag / HealthRecord / AmbientDisplay 等 21 个旧文件）+ 5-tab 骨架
- [x] Phase 2 — Room schema + 默认数据 seeder（42 分类 + 6 设置项）
- [x] Phase 3 — 今日账本时间线 + 长按拖拽 reorder + cascade shift
- [x] Phase 4 — 记一笔单表单 + tap-to-edit + 越界拒绝
- [x] Phase 5 — 统计 4 范围（日/周/月/年）+ 一/二级分类条形展开
- [x] Phase 6 — 复盘日/周/月模板 + 历史列表
- [x] Phase 7 — 设置：分类/项目管理 + 默认起点 + CSV/MD/JSON 导出 + JSON 恢复
- [x] Phase 8 — 暗色主题适配 + 锁竖屏 + 新启动图标 + Lint/test 通过 + README 重写

**v1.1（2026-06-21）**：4 项 UX 维护 — Tab 重排 + 中间 TODAY FAB / 顶层 BackHandler / DurationInput 三 chip + 双轮 wheel / 默认分类 5+13（destructive migration 一次性破例）。

**v1.2（2026-06-27）**：5 大诉求 + 配套迭代（详见 `docs/v1.2-plan-2026-06-26.md` 与 `docs/v1.2-summary-2026-06-27.md`）：

- [x] §1 记一笔滑选式重写 — `ChoiceWheel` 横向滑选 + 日期去 label + 标题派生 + 备注改 singleLine + EffectivenessRow slider
- [x] §2 AI 复盘 — DeepSeek 集成（OkHttp + INTERNET + JSON mode + 双周期 snapshot + 首次隐私 dialog + Settings 子页测试连接）
- [x] §3 TODAY 尾部「+」占位 + 一键插 10min 间隔 + v1.2 seed（讨论 + 间隔）+ Room v2→v3 加 effectiveness 列（显式 Migration）
- [x] §4 STATS 默认本周 + 头部右上周期 + 去重 rangeToDates
- [x] §5 分类调色板可视化 — ColorSwatchPicker 色块网格替换 8 个文字按钮
- [x] 配套 — Launcher icon 美术 / 亮暗 palette 翻转 / TimelineBlock 文字色自适应 / effectiveness-weighted 统计聚合

**v1.3（2026-06-27）**：2 大诉求 + 3 次 STATS 滑动 bug 修复 + 同日 post-release fix（详见 `docs/v1.3-plan-2026-06-27.md` 与 `docs/v1.3-summary-2026-06-27.md`）：

- [x] §1 STATS 屏加左右滑动浏览历史 — Dense Pager（`pageCount = anchors.size`，空时段直接跳过）+ anchor 记忆（entry 增删不被动跳页）+ 下方追加 read-only「本X复盘」显示区（YEAR 时整段隐藏）
- [x] §1 配套 — REVIEW 屏加日期选择器（解决 STATS 看到「去 REVIEW tab 写」却无法选历史时段的痛点）
- [x] §1 bug 修复 — 滑动方向反 / `settledPage` 替代 `currentPage` 止动画争用 / `LaunchedEffect(key)` 替代 `snapshotFlow { StateFlow.value }`（后者不跟踪 StateFlow）
- [x] §2 AI prompt 可编辑 + `{period}` 占位符 + `JSON_FORMAT_SUFFIX` 始终追加
- [x] §2 复盘上下文窗口数字配置（DAY 1–14 / WEEK 1–7）+ MONTH 复盘对话框勾选本周涉及周报
- [x] §2 预览功能 — PreviewDialog 滚动展示完整 system + user JSON + 「复制全部」
- [x] §2 snapshot 重构 — 移除 previous entries snapshot，只发当前周期 entries + `recentReviews: List<ReviewEntity>`
- [x] 发布后修 — DatePicker 用 UTC 时区（原 systemDefault 在 UTC+8 让 focus 跳昨天）+ REVIEW 屏预览融入生成流程作二次确认（`PendingPreviewData` 保证「所见即所传」，独立预览按钮已删）

**v1.4 候选**：API key 加密（EncryptedSharedPreferences）/ Release signing + ProGuard keep / `fetchRecentReviews` 抽 helper 去重（Settings + Review VM 现有 ~25 行重复）/ MONTH dialog 显示每周 summaryText 预览 / STATS anchor 被删空时跳到相邻而非最新。

## 分类持久化行为

`DefaultDataSeeder` 由 `KEY_SEEDED` AppSetting 守护，**仅在首启 seed 一次**。用户在设置页对分类做的修改（重命名、归档、新增）**在 app 更新后保留**，不会被覆盖。

**例外**：v1.1（2026-06-21）一次性借 `fallbackToDestructiveMigration` 把 Room v1 DB 整库清掉重建，用于套用新的 5 一级 + 13 二级 seed 列表（用户授权，旧数据是测试用）。后续版本若再改 schema 必须写显式 Migration。

## 项目结构

```
app/src/main/java/com/example/timemanager/
├── TimeManagerApplication.kt        Application + ViewModelStoreOwner（5 个 VM 挂这）
├── data/
│   ├── DefaultDataSeeder.kt         首启 seed 42 分类 + day_start_min=480
│   ├── converter/Converters.kt      LocalDate/LocalDateTime ↔ epoch
│   ├── dao/                          TimeEntry / Category / Project / Review / AppSetting
│   ├── db/AppDatabase.kt            Room v1 单例
│   └── entity/                      5 个 @Entity
├── ui/
│   ├── activities/MainActivity.kt   5-tab NavigationBar + Screen enum
│   ├── components/                  CategoryColors / CategoryPicker / CategoryBar
│   │                                DayTimeline / TimelineBlock / DurationInput
│   ├── screens/                     TodayLedger / Record / Stats / Review / Settings
│   │                                + CategoryManager / ProjectManager 子页
│   └── theme/                       Color / Type / Theme（M3，跟随系统暗色）
├── util/                            TimeText / DateRange / CsvExporter
└── viewmodel/                       TodayLedger / Record / Stats / Review / Settings
                                    全部 Application scoped
```

数据层 5 张表：`time_entries` / `categories` / `projects` / `reviews` / `app_settings`。

## 构建与运行

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 安装到连接的设备 / 模拟器
./gradlew installDebug

# 启动 MainActivity
adb shell am start -n com.example.timemanager/.ui.activities.MainActivity

# JVM 单元测试
./gradlew test

# Android Lint
./gradlew lint
```

### 本机工具链

| 工具 | 路径 |
|---|---|
| JDK 21（Android Studio JBR） | `D:/Work/Androids/Studio/jbr/bin/java` |
| Android SDK | `C:/Users/laozihao/AppData/Local/Android/Sdk` |
| `adb.exe` | `C:/Users/laozihao/AppData/Local/Android/Sdk/platform-tools/adb.exe` |
| `compileSdk` / `targetSdk` | 36 |
| `minSdk` | 24（启用 core library desugaring 支持 `java.time`） |
| 物理设备 | Xiaomi 2407FRK8EC，Android 16 |

## 关键不变量（贡献前必读）

详见 `CLAUDE.md`，简述：

1. **`TimerViewModel` 旧体系已删**。新 ViewModel 挂在 `TimeManagerApplication` 的 `appViewModelStore` 上，跨 Activity 重建存活 —— 不要把 ViewModel scope 到 Activity。
2. **duration-first**。`TimeEntry` 的 `startMinOfDay` 由"当天已有 entry 的最大 endMinOfDay"派生（当天空时取 `day_start_min`），用户不输入开始时间。
3. **block 默认 back-to-back 紧贴**。不做空白段保留；过渡时段要么显式建 entry，要么被前后段吸收。
4. **拖拽同步写库**，由 Mutex 序列化 reorder 操作避免竞态。
5. **不做实时计时器 / 健康提醒 / Ambient Mode / 完整率指标**。这些是 v0 旧设计，已彻底删除。

## 技术栈

- Kotlin + Coroutines
- Jetpack Compose + Material3（跟随系统暗色）
- Room 2.6.1 + KSP 2.0.21-1.0.27
- 手写 `Screen` 枚举 + `rememberSaveable` + `BackHandler` 导航（无 Navigation Compose / Hilt）
- SAF（Storage Access Framework）做文件导出 / 恢复
