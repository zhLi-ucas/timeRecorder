# TimeManager Project Documentation

## 简介
TimeManager 是一款 Android 时间管理应用，基于 Jetpack Compose 开发。它旨在帮助用户通过倒计时和正计时（秒表）功能来追踪和管理时间，提供沉浸式的横屏时钟体验，并具备详细的时间记录与统计功能。

## 当前功能 (Current Features)

1.  **多模式计时**
    *   **倒计时 (Timer)**: 支持选择预设时长（如 15分钟、1小时）进行专注计时。
    *   **正计时 (Stopwatch)**: 支持无限制的正向计时，适合记录不确定时长的任务。
    *   **沉浸式体验**: 计时过程中进入横屏全屏模式，屏幕常亮，显示当前时间、日期及任务进度。

2.  **标签系统 (Tag System)**
    *   预设标签（Working, Learning, Playing）。
    *   支持创建自定义标签，自动分配随机颜色。
    *   支持删除不再使用的标签。

3.  **时长管理 (Duration Management)**
    *   管理常用的倒计时时长预设。
    *   支持添加自定义时长和删除现有预设。

4.  **时间记录 (Time Records)**
    *   **自动记录**: 任务结束或停止时自动保存记录。
    *   **智能记录规则**:
        *   正计时：记录实际用时。
        *   倒计时（顺利结束）：记录完整预设时长。
        *   倒计时（提前结束）：按 5 分钟向下取整记录（例如 14分钟 -> 10分钟），不足 5 分钟不记录。
    *   **历史查看**: 按日期分组展示历史记录，包含标签、具体时间段、时长和任务描述。

5.  **数据持久化**
    *   标签和时长配置通过 SharedPreferences 本地存储。
    *   时间记录通过 JSON 文件本地存储，确保数据不丢失。

---

## 项目结构说明 (Project Structure)

项目源代码位于 `app/src/main/java/com/example/timemanager/`，主要采用 MVVM 架构。

### 1. UI 层 (`ui/`)

负责应用的界面展示和交互。

*   **Activities (`ui/activities/`)**
    *   [`MainActivity.kt`](app/src/main/java/com/example/timemanager/ui/activities/MainActivity.kt): 应用的主入口。使用 Compose Navigation 管理应用内的主要屏幕导航（主页、设置页、选项页、记录页）。
    *   [`AmbientDisplayActivity.kt`](app/src/main/java/com/example/timemanager/ui/activities/AmbientDisplayActivity.kt): 独立的横屏 Activity。负责显示计时界面，配置了全屏沉浸模式和屏幕常亮标志 (`FLAG_KEEP_SCREEN_ON`)。

*   **Screens (`ui/screens/`)**
    *   [`HomeScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/HomeScreen.kt): 应用主页，提供三大功能入口：开始计时、选项设置、查看记录。
    *   [`TimerSetupScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/TimerSetupScreen.kt): 任务配置页。用户在此选择标签、模式、时长并输入任务描述。
    *   [`AmbientDisplayScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/AmbientDisplayScreen.kt): 计时核心界面。显示大字号时钟、倒计时/秒表读数，提供“停止/关闭”悬浮按钮。
    *   [`OptionsScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/OptionsScreen.kt): 设置页。用于管理（添加/删除）标签和时长预设。
    *   [`TimeRecordsScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/TimeRecordsScreen.kt): 历史记录页。使用 LazyColumn 和 StickyHeader 实现按日期的分组展示。

### 2. 逻辑层 (`viewmodel/`)

负责连接 UI 和数据，处理业务逻辑。

*   [`TimerViewModel.kt`](app/src/main/java/com/example/timemanager/viewmodel/TimerViewModel.kt): 
    *   **核心控制器**: 整个应用的单例 ViewModel（Application Scope）。
    *   **状态管理**: 管理计时器状态 (`TimerState`)、当前任务、标签列表、历史记录等 `StateFlow`。
    *   **计时逻辑**: 使用 Kotlin Coroutines 实现倒计时和秒表的每秒更新逻辑。
    *   **数据桥接**: 调用 Repository 层进行数据的加载和保存。
    *   **业务规则**: 实现了“倒计时提前结束向下取整”的特定业务逻辑。

### 3. 数据层 (`data/`)

负责数据的定义和持久化存储。

*   **Models (数据模型)**
    *   [`Task.kt`](app/src/main/java/com/example/timemanager/data/Task.kt): 运行时任务对象，包含临时状态（如是否为秒表模式）。
    *   [`TimeRecord.kt`](app/src/main/java/com/example/timemanager/data/TimeRecord.kt): 持久化的历史记录对象，包含 UUID、起止时间、实际时长等。
    *   [`Tag.kt`](app/src/main/java/com/example/timemanager/data/Tag.kt): 标签对象，包含名称和颜色值。

*   **Repositories (数据仓库)**
    *   [`TagRepository.kt`](app/src/main/java/com/example/timemanager/data/TagRepository.kt): 封装 SharedPreferences，管理标签数据的存取。
    *   [`DurationRepository.kt`](app/src/main/java/com/example/timemanager/data/DurationRepository.kt): 封装 SharedPreferences，管理时长预设的存取。
    *   [`TimeRecordRepository.kt`](app/src/main/java/com/example/timemanager/data/TimeRecordRepository.kt): 封装 File IO，使用 JSON 格式在内部存储中读写历史记录列表。

### 4. 服务与基础设施

*   [`TimeManagerApplication.kt`](app/src/main/java/com/example/timemanager/TimeManagerApplication.kt): 全局 Application 类，实现了 `ViewModelStoreOwner`，确保 ViewModel 可以在不同 Activity 间保持单例状态。
*   [`NotificationService.kt`](app/src/main/java/com/example/timemanager/service/NotificationService.kt): 处理计时结束时的系统通知发送。

---

## 技术栈 (Tech Stack)

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material3)
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Concurrency**: Kotlin Coroutines & Flow
*   **Persistence**: 
    *   SharedPreferences (Settings/Config)
    *   JSON File Storage (Large Datasets)
