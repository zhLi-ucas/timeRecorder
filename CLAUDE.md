# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TimeManager is an Android time-tracking app built with Jetpack Compose + Material3. The core design principle is **"start/end point" time tracking rather than a constantly-running background service**: a task records only its start timestamp (persisted to `SharedPreferences`), and the duration is computed from `System.currentTimeMillis()` deltas when the user stops the task. This lets timing survive app/process death without a foreground service.

The app also bundles health reminders (water and stand-up), a tag system with home-page visibility, a history view, and a dedicated landscape "Ambient Mode" full-screen clock activity.

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
| AVDs configured | **none** — `~/.android/avd` is empty. Use the connected physical device, or run `avdmanager create avd` first if a simulator is needed. |

A bash helper to avoid typing the full adb path in a session:

```bash
ADB="C:/Users/laozihao/AppData/Local/Android/Sdk/platform-tools/adb.exe"
```

### Deploy & inspect on device

```bash
# Build + install in one shot
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

### UI review loop (no MCP required)

To "see" the running app and iterate on UI without leaving Claude Code:

```bash
# Capture the current screen to a PNG
$ADB exec-out screencap -p > /tmp/screen.png

# Then analyze it with the analyze_image tool against a remote URL,
# or pull and inspect locally.
```

Note: `mcp__4_5v_mcp__analyze_image` only accepts a **remote URL**, not a local path. For local screenshots, either upload the PNG to a host the session can reach, or describe the issue in text.

### Toolchain notes
- `compileSdk`/`targetSdk` = 36, `minSdk` = 24, Java/JVM target = 11 (the build JRE is 21; Kotlin/Java targets stay at 11 — don't bump without coordinated changes in `app/build.gradle.kts`).
- Aliyun mirrors are **first-class** in `settings.gradle.kts` (with `google()`/`mavenCentral()` as fallback). Don't reorder or remove them — the build machine relies on them for download speed. Any new repository additions should follow the same pattern.
- Version catalog lives at `gradle/libs.versions.toml`. Add new deps there, not inline.
- `local.properties` (gitignored) must contain `sdk.dir` — already configured on this machine; do not commit.
- The `release` build type has **no signing config** — `assembleRelease` will produce an unsigned APK. Add a `signingConfigs` block before attempting release distribution.
- No Android-specific MCP is registered in this session; the only MCP tools available are `mcp__4_5v_mcp__analyze_image` (image URL → vision) and `mcp__web_reader__webReader` (URL → markdown).

## Architecture

### Process-scoped ViewModel via Application scope

`TimeManagerApplication` implements `ViewModelStoreOwner` and exposes a single `appViewModelStore`. `MainActivity.AppContent()` (and any future entry) obtains `TimerViewModel` scoped to the **Application**, not the Activity:

```kotlin
val timerViewModel: TimerViewModel = viewModel(
    viewModelStoreOwner = application as ViewModelStoreOwner,
    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
)
```

This is intentional — `TimerViewModel` must survive Activity recreation (config changes, Ambient↔Main navigation, back stack) to keep the running task state and reminder loop alive. **Do not scope `TimerViewModel` to an Activity** or you'll lose the running timer on rotation.

### In-app navigation

Navigation is hand-rolled via the `Screen` enum in `MainActivity.kt` (`HOME`, `OPTIONS`, `RECORDS`) plus `rememberSaveable` state — **not** Navigation Compose. `AmbientDisplayActivity` is a separate Activity (landscape, fullscreen, `FLAG_KEEP_SCREEN_ON`) reached via `Intent`. When adding new screens, follow the same enum + `BackHandler` pattern unless migrating to Navigation Compose.

### `TimerViewModel` is the source of truth

`viewmodel/TimerViewModel.kt` owns all state and is intentionally large. Key invariants:

- **Ongoing task is persisted in `SharedPreferences` ("timer_prefs")**, keys prefixed `ongoing_*`. On `init`, `checkOngoingTask()` reconstructs a `Task` and re-arms the UI ticker if `KEY_START_TIME != -1L`. This is how the timer survives app kill.
- Two coroutine jobs: `timerJob` (ticks `_displaySeconds` every 1s) and `reminderJob` (polls every 10s — see comment about trading precision for battery).
- Tags are split into two derived `StateFlow`s via `map{}.stateIn(SharingStarted.WhileSubscribed(5000))`:
  - `displayTags` = `tag.showOnHome == true` — drives the `HorizontalPager` on `HomeScreen`.
  - `otherTags` = the rest — selectable via the "其他" fallback page.
- `HomeScreen` syncs pager position → `selectTag()` via `LaunchedEffect(displayTags, pagerState.currentPage)`. The pager has `pageCount = displayTags.size + 1` (the +1 is the "other" page).
- UI events (e.g. `ShowSaveRecordDialog`) flow through a `Channel` → `receiveAsFlow()` rather than `SharedFlow`, so the consumer pattern in `HomeScreen` uses `collectLatest`. Keep this channel-based pattern when adding one-shot UI events.
- `endTask()` does **not** save the record directly — it emits `UiEvent.ShowSaveRecordDialog` so the user can edit the description first. The dialog then calls `addRecord`.

### Data layer

All repositories are constructed in `TimerViewModel` with `application` as the `Context` — **not** injected (no Hilt/Dagger in this project). Two storage strategies coexist:

| Repo | Storage | File/Key |
|---|---|---|
| `TimeRecordRepository` | JSON file | `filesDir/time_records.json` |
| `HealthRecordRepository` | JSON file | `filesDir/health_records.json` |
| `TagRepository` | SharedPreferences | `time_manager_tags` → `saved_tags` (JSON string) |
| `DurationRepository` | SharedPreferences | `time_manager_durations` → `saved_durations` (JSON array) |
| `TimerViewModel` (timer/reminder state) | SharedPreferences | `timer_prefs` (mixed scalar keys) |

Repositories read the whole file/pref on every mutation (`getAllRecords().toMutableList()` then re-serialize). Acceptable at current scale; do not pre-optimize unless a perf issue is observed.

### Time model

`Task` (data/Task.kt) is the in-memory representation while running; `TimeRecord` (data/TimeRecord.kt) is the persisted completed-task shape with `creationType` (default `"QUICK"`, also `"NORMAL"`). `isStopwatch` on `Task` gates whether `endTime` is computed — the stopwatch path is the live one; the fixed-duration path is legacy.

### Notifications

`service/NotificationService` is a singleton `object`, not a bound service. `ReminderType` (WATER / STAND) is defined in `ui/components/` (referenced from both ViewModel and NotificationService). Two notification channels: timer completion and reminders. Notification debounce is done via `last_notify_${type}` prefs key (60s window) inside `TimerViewModel.checkAndNotify`, not in the notification service itself.

## Conventions specific to this codebase

- **Language mix**: UI strings, comments, and notification copy are mostly **Simplified Chinese**; identifiers are English. Match the surrounding file's language when adding content.
- **No DI framework** — instantiate repositories directly in the ViewModel constructor with `application` as context.
- **Compose UI lives in `ui/screens`** (top-level screens), **`ui/components`** (reusable composables like `StartButton`, `ThermometerReminder`, `ReminderButton`, `FinishTaskDialog`, `TagSelectionDialog`), and **`ui/activities`** (Activity shells).
- **Long-press is the primary timer action**: `StartButton` uses `detectTapGestures` with a fill animation on long press to start. Don't change to single-tap without coordinating with the rest of the home flow.
- The README has a "重大版本重构与实施规划" section with empty `step1/step2/...` checklists — this is a living planning doc, update it when scoping a major refactor.

## Where to look for what

- Want to change home-screen behavior → `ui/screens/HomeScreen.kt` (pager, dialogs, reminder wiring).
- Want to change what survives an app restart → `TimerViewModel` `companion object` keys + `checkOngoingTask()` / `startTask()` / `endTask()`.
- Want a new tag field → `data/Tag.kt` + serialization in `TagRepository.kt` (both `getTags()` and `saveTags()`).
- Want a new screen → add to `Screen` enum in `MainActivity.kt` and wire a `when` branch + `BackHandler`.
