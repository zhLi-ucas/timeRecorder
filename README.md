# TimeManager

基于**柳比歇夫时间统计法**的 Android 时间账本 App（Jetpack Compose + Material3 + Room）。

录入只填**分类 + 时长**，时间线靠默认堆叠（紧贴 back-to-back）+ 长按拖拽 reorder；不做实时计时器、不做健康提醒、不做 todo —— 只做时间账本 + 分类统计 + 周期复盘 + 数据导出。

## 核心特性

- **Duration-first 录入**：选日期 → 一级/二级分类 → 时长（chip + 自定义）→ 标题（可选）→ 保存，30 秒一条
- **今日时间线**：block 默认从默认起点（08:00）紧贴堆叠；点击编辑、长按菜单删除
- **长按拖拽 reorder**：5 分钟 snap + cascade shift（重叠时下方 block 整体后移）；越过 24:00 拒绝
- **统计聚合**：日/周/月/年 4 范围；一/二级分类横向条形 + 百分比
- **周期复盘**：日 / 周 / 月三套模板字段，历史可回溯
- **分类 / 项目管理**：8 默认一级 + 34 二级，可归档、可新增；项目作为可选统计维度
- **导出 / 备份**：CSV / Markdown 报表 + JSON 全量备份与恢复
- **暗色主题**：跟随系统，分类色板 light/dark 双色
- **持久化**：Room (SQLite)，App 重启 / Activity 重建均不丢状态

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

**v1.1 候选**：自定义统计范围、分类拖拽排序、分类调色色板 UI、记一笔项目字段、年度按月柱状图、ProGuard 规则与 release 签名。

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
