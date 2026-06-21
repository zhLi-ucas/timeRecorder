# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TimeManager 是一款基于**柳比歇夫时间统计法**的 Android 时间账本 App（Jetpack Compose + Material3 + Room）。录入只填**分类 + 时长**（不输入开始时间），时间线靠默认堆叠 + 长按拖拽 reorder；不做实时计时器、不做健康提醒、不做 todo。

5 个一级 tab（顺序固定）：**记一笔 (RECORD) → 复盘 (REVIEW) → 今日账本 (TODAY) → 统计 (STATS) → 设置 (SETTINGS)**，中间位 TODAY 是常驻 FAB 浮起按钮。首启默认进 TODAY。

当前版本：**v1.1**（2026-06-21）。Phase 1–8（v1）+ v1.1 维护全部完成。

## Build & Run

All Gradle commands use the wrapper. On Windows the project still targets bash-style invocation.

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests (JVM)
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.example.timemanager.ExampleUnitTest"

# Run instrumented (on-device) tests
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

### Verified toolchain locations (this machine)

The whole toolchain is installed and tested — Claude Code can build, deploy, screenshot, and read logs directly without any MCP. Paths are absolute because the global `ANDROID_HOME` env var is **not** set; `local.properties` carries `sdk.dir` instead, which is enough for Gradle but not for invoking `adb`/`emulator` from a shell.

| Tool | Path |
|---|---|
| JDK (Java 21, Android Studio JBR) | `D:/Work/Androids/Studio/jbr/bin/java` (already on PATH) |
| Android SDK root | `C:/Users/laozihao/AppData/Local/Android/Sdk` |
| `adb.exe` (v1.0.41) | `C:/Users/laozihao/AppData/Local/Android/Sdk/platform-tools/adb.exe` |
| `emulator.exe` | `C:/Users/laozihao/AppData/Local/Android/Sdk/emulator/emulator.exe` |
| Build-tools | 34.0.0, 35.0.0, 36.1.0 |
| Platforms installed | android-34, android-36 |
| AVDs configured | **none** — `~/.android/avd` is empty. Use the connected physical device (Redmi 2407FRK8EC), or run `avdmanager create avd` first if a simulator is needed. |

A bash helper to avoid typing the full adb path in a session:

```bash
ADB="C:/Users/laozihao/AppData/Local/Android/Sdk/platform-tools/adb.exe"
```

### Deploy & inspect on device

```bash
# Build + installation in one shot
./gradlew :app:installDebug

# Or manual install of an existing APK
$ADB install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the launcher activity
$ADB shell am start -n com.example.timemanager/.ui.activities.MainActivity

# Crash + activity logs
$ADB logcat -s AndroidRuntime:E ActivityManager:I

# List connected devices
$ADB devices
```

### DB inspection (when MIUI blocks sqlite3 binary)

Device `sqlite3` is not accessible under `run-as`. Pull the DB + WAL + SHM and read with Python:

```bash
$ADB exec-out run-as com.example.timemanager cat databases/timemanager.db     > /tmp/tm.db
$ADB exec-out run-as com.example.timemanager cat databases/timemanager.db-wal  > /tmp/tm.db-wal
$ADB exec-out run-as com.example.timemanager cat databases/timemanager.db-shm  > /tmp/tm.db-shm
python -c "import sqlite3; c=sqlite3.connect('/tmp/tm.db'); print(list(c.execute('SELECT id,name FROM categories')))":
```

All three files must be pulled together — recent writes may still be in the WAL.

### UI review loop (no MCP required)

`mcp__4_5v_mcp__analyze_image` only accepts a **remote URL**, not a local path. Screencap PNGs go through a CDN with cache keyed on path — repeated `screencap > same/path.png` returns the same URL, so visual verification from this side is unreliable. Rely on:

- `adb shell dumpsys activity activities | grep topResumedActivity` for "did the app crash / is it foregrounded"
- User actually running the app on the device for visual confirmation

### Toolchain notes
- `compileSdk`/`targetSdk` = 36, `minSdk` = 24, Java/JVM target = 11 (the build JRE is 21; Kotlin/Java targets stay at 11 — don't bump without coordinated changes in `app/build.gradle.kts`).
- **Core library desugaring is enabled** (`desugar_jdk_libs:2.1.5`), so `java.time.*` works on minSdk 24. Don't remove this — large parts of the data layer depend on `LocalDate`/`LocalDateTime`.
- Aliyun mirrors are **first-class** in `settings.gradle.kts` (with `google()`/`mavenCentral()` as fallback). Don't reorder or remove them — the build machine relies on them for download speed. Any new repository additions should follow the same pattern.
- Version catalog lives at `gradle/libs.versions.toml`. Add new deps there, not inline.
- `local.properties` (gitignored) must contain `sdk.dir` — already configured on this machine; do not commit.
- The `release` build type has **no signing config** — `assembleRelease` will produce an unsigned APK. Add a `signingConfigs` block before attempting release distribution.
- No Android-specific MCP is registered in this session; the only MCP tools available are `mcp__4_5v_mcp__analyze_image` (image URL → vision) and `mcp__web_reader__webReader` (URL → markdown).

## Architecture

### Process-scoped ViewModel via Application scope

`TimeManagerApplication` implements `ViewModelStoreOwner` and exposes a single `appViewModelStore`. `MainActivity.AppContent()` obtains every ViewModel scoped to the **Application**, not the Activity:

```kotlin
val todayVm: TodayLedgerViewModel = viewModel(
    viewModelStoreOwner = application as ViewModelStoreOwner,
    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
)
```

This is intentional — ViewModels must survive Activity recreation (config changes, navigation, back stack). **Do not scope any ViewModel to an Activity** or you'll lose state on rotation.

5 Application-scoped ViewModels: `TodayLedgerViewModel`, `RecordViewModel`, `StatsViewModel`, `ReviewViewModel`, `SettingsViewModel`.

### In-app navigation

5-tab bottom navigation is hand-rolled via the `Screen` enum in `MainActivity.kt` + `rememberSaveable` state — **not** Navigation Compose. Order is fixed:

```kotlin
enum class Screen(val label: String, val icon: ImageVector) {
    RECORD, REVIEW, TODAY, STATS, SETTINGS
}
```

The bottom bar is a **custom composable** (`HomeBottomBar.kt`), not Material3 `NavigationBar`, because the middle slot (TODAY) is a常驻 `FloatingActionButton` that protrudes 16dp above the bar. Don't switch back to `NavigationBar` without losing the FAB styling.

`BackHandler` at the top of `AppContent` ensures any non-TODAY tab back/swipe returns to TODAY:
```kotlin
BackHandler(enabled = currentScreen != Screen.TODAY || settingsSubpage != SettingsSubpage.ROOT) { ... }
```
This makes TODAY the "home" tab. Don't add per-screen BackHandlers without coordinating with this top-level one — Compose's nested priority means child handlers override.

Settings has 3 subpages (`ROOT` / `CATEGORIES` / `PROJECTS`) tracked by a separate `settingsSubpage` state.

### Data layer (Room v2)

All repositories are constructed in ViewModels with `application` as the `Context` — **not** injected (no Hilt/Dagger). 5 entities:

| Entity | Table | Notes |
|---|---|---|
| `TimeEntryEntity` | `time_entries` | Core record: `date`, `startMinOfDay`, `durationMin`, `categoryId`, `projectId?`, `note?` |
| `CategoryEntity` | `categories` | Two-level: `parentId == null` = 一级; `colorKey` lives on 一级 only; `isSystem` guards the undeletable "其他" |
| `ProjectEntity` | `projects` | Optional statistical dimension |
| `ReviewEntity` | `reviews` | DAY/WEEK/MONTH templates |
| `AppSettingEntity` | `app_settings` | Key-value (PK = key string) |

`AppDatabase` is `version = 2` with **`fallbackToDestructiveMigration()`** (v1.1 one-time exception, user authorized for the seed-list revision). **Future schema changes must ship explicit `Migration` objects** — destructive is now closed.

`DefaultDataSeeder` runs once on first launch (gated by `KEY_SEEDED` AppSetting). It seeds **5 一级 + 13 二级** categories:

| 一级 | colorKey | 二级 |
|---|---|---|
| 工作 | blue | 实验 / 阅读 / 写论文 / 思考 |
| 休息 | cyan | 放空 / 游戏 / 阅读 |
| 自我实现 | orange | 规划 / 户外 / 室内 |
| 无效消耗 | grey | 无效等待 / 被动阅读 |
| 其他 (isSystem) | neutral | 待分类 |

User edits to categories (rename / archive / add) **persist across app updates** because the seeder only runs once. Don't re-seed on every launch.

### Time model

`TimeEntryEntity` is the persisted record. `Task`/`TimeRecord` from the old v0 design are **deleted** — don't reintroduce. The `startMinOfDay` is **derived** (max end-of-day for the date, falling back to `day_start_min` AppSetting = 480 / 08:00), never input by the user. User input is only `category + duration`.

### Duration input

`DurationInput.kt` exposes 3 chips: **30m / 60m / 自定义**. The custom chip opens `DurationPickerSheet` (a `ModalBottomSheet`) containing two `WheelPicker` components (hours 0–23 + minutes 0/5/.../55).

`WheelPicker` is a hand-rolled Compose wheel using `LazyColumn` + `rememberSnapFlingBehavior` — M3 has no built-in. Don't swap for a text field; the previous text-field design had a state-coupling bug where typing "150" visually snapped to the "15m" chip.

### Timeline drag-reorder

`DayTimeline.kt` long-presses a block → drag → 5-min snap → cascade shift (overlapping downstream blocks pushed forward). Crossing 24:00 → reject + toast. The reorder operation is serialized via a Mutex inside `TodayLedgerViewModel` to avoid races. Don't remove the Mutex.

## Conventions specific to this codebase

- **Language mix**: UI strings, comments, notification copy, and default category names are **Simplified Chinese**; identifiers are English. Match the surrounding file's language when adding content.
- **No DI framework** — instantiate repositories directly in the ViewModel constructor with `application` as context.
- **Compose UI lives in `ui/screens`** (top-level screens), **`ui/components`** (reusable composables: `HomeBottomBar`, `WheelPicker`, `DurationPickerSheet`, `DurationInput`, `CategoryPicker`, `CategoryBar`, `DayTimeline`, `TimelineBlock`, `CategoryColors`), and **`ui/activities`** (Activity shells).
- **底栏始终可见**（v1.1 改动）— RECORD 屏现在也是普通 tab，不是模态；之前隐藏底栏的逻辑已删除。
- **`CategoryColors.colorFor(key)` is `@Composable`** — it switches between light/dark palettes via `isSystemInDarkTheme()`. All 3 call sites are inside Composition. Don't call from non-Composable contexts.

## Where to look for what

- Change bottom bar / tab order → `MainActivity.kt` (`Screen` enum + `HomeBottomBar` invocation) and `ui/components/HomeBottomBar.kt`
- Change back behavior → `MainActivity.kt` top-level `BackHandler`
- Change default categories → `data/DefaultDataSeeder.kt` (and bump Room version + add Migration, don't use destructive again)
- Change category colors → `ui/components/CategoryColors.kt` (lightPalette / darkPalette maps)
- Change duration input → `ui/components/DurationInput.kt` + `DurationPickerSheet.kt` + `WheelPicker.kt`
- Change timeline / drag-reorder → `ui/screens/TodayLedgerScreen.kt` + `ui/components/DayTimeline.kt` + `TodayLedgerViewModel`
- Change stats aggregation → `viewmodel/StatsViewModel.kt` + `util/DateRange.kt`
- Change export format → `util/CsvExporter.kt` + `viewmodel/SettingsViewModel.kt` (Markdown + JSON backup)

The README has a "重大版本重构与实施规划" section reflecting v1 / v1.1 — update it when scoping future work.

## Session docs

- `docs/plan.md` — original 8-phase plan (Phase 0–8 all `[x]`)
- `docs/gpt_plan.md` — product design blueprint (Lyubishchev method theory)
- `docs/sessions/2026-06-20_phase1-7.md` — Phase 1–7 session log
- `docs/sessions/2026-06-21_phase8.md` — Phase 8 session log
- `docs/sessions/2026-06-21_v1.1.md` — v1.1 maintenance session log
- `docs/sessions/2026-06-21_today-history.md` — TODAY 历史浏览与编辑 session log
- `docs/v1.1-summary-2026-06-21.md` — v1.1 retrospective (this work)
- `docs/v1.1-maintenance-2026-06-21.md` — v1.1 user-drafted plan (design rationale)
