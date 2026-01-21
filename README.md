# TimeManager Project Documentation

## 简介
TimeManager 是一款 Android 时间管理应用，基于 Jetpack Compose 开发。它摒弃了传统的后台即时计时模式，采用更优雅的“起止点”记录方案，横屏时钟体验，帮助用户轻松追踪时间。同时内置喝水与久坐提醒功能，关注用户健康。

## 重大版本重构与实施规划 (Major Refactoring & Implementation Plan)

### 前端修改要求 (Frontend Requirements)
1. 暂无
   - [ ] step1
   - [ ] step2
   - [ ] step3
   - [ ] ...

### 后端修改要求 (Backend Requirements)
*(注：此处后端指代应用内部数据层与逻辑层，或将来扩展的服务端)*

1. 暂无
   - [ ] step1
   - [ ] step2
   - [ ] step3
   - [ ] ...

## 当前功能 (Current Features)

*   **时间追踪 (Time Tracking)**:
    *   **快速启动**: 长按开始按钮直接以当前选中的标签开始计时。
    *   **滑动选择**: 主页支持左右滑动切换常用的“展示标签 (Show Tags)”。
    *   **二级选择**: 提供“其他”选项，支持从完整列表中选择标签。
    *   **完成确认**: 停止计时后自动弹出对话框，用于补充任务描述。
    *   支持后台运行（基于时间戳计算，无需常驻后台服务）。
*   **健康提醒 (Health Reminders)**:
    *   内置“喝水”与“久坐”提醒。
    *   可视化进度展示：
        *   **温度计 (Thermometer)**: 动态显示提醒倒计时进度。
        *   **进度按钮 (ReminderButton)**: 圆形进度指示。
*   **历史记录 (History)**:
    *   按时间倒序查看所有任务记录。
    *   本地 JSON 持久化存储。
*   **标签系统 (Tag System)**:
    *   自定义任务标签（Working, Learning, Playing 等）。
    *   **首页展示管理**: 在设置中可配置哪些标签显示在主页滑动列表中。
    *   支持标签颜色管理。
*   **专注模式 (Ambient Mode)**:
    *   提供纯净的横屏时钟显示 (`AmbientDisplayActivity`)。

## 项目结构说明 (Project Structure)

项目源代码位于 `app/src/main/java/com/example/timemanager/`，采用标准的 MVVM 架构。

### 1. UI 层 (`ui/`)

*   **Activities (`ui/activities/`)**
    *   [`MainActivity.kt`](app/src/main/java/com/example/timemanager/ui/activities/MainActivity.kt): 应用主入口。
    *   [`AmbientDisplayActivity.kt`](app/src/main/java/com/example/timemanager/ui/activities/AmbientDisplayActivity.kt): 独立的专注模式 Activity。

*   **Screens (`ui/screens/`)**
    *   [`HomeScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/HomeScreen.kt): 主页，集成 `HorizontalPager` 实现标签滑动选择，包含计时控制与提醒。
    *   [`TimeRecordsScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/TimeRecordsScreen.kt): 历史记录列表。
    *   [`OptionsScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/OptionsScreen.kt): 设置页，包含标签管理（新增首页展示开关）。
    *   [`AmbientDisplayScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/AmbientDisplayScreen.kt): 专注模式 UI 实现。

*   **Components (`ui/components/`)**
    *   [`StartButton.kt`](app/src/main/java/com/example/timemanager/ui/components/StartButton.kt): 计时控制按钮，支持长按启动。
    *   [`FinishTaskDialog.kt`](app/src/main/java/com/example/timemanager/ui/components/FinishTaskDialog.kt): 任务结束时的编辑对话框。
    *   [`ThermometerReminder.kt`](app/src/main/java/com/example/timemanager/ui/components/ThermometerReminder.kt): 垂直温度计样式的提醒进度条。
    *   [`ReminderButton.kt`](app/src/main/java/com/example/timemanager/ui/components/ReminderButton.kt): 圆形提醒按钮。
    *   [`TagSelectionDialog.kt`](app/src/main/java/com/example/timemanager/ui/components/TagSelectionDialog.kt): 通用标签选择对话框。

### 2. 逻辑层 (`viewmodel/`)

*   [`TimerViewModel.kt`](app/src/main/java/com/example/timemanager/viewmodel/TimerViewModel.kt):
    *   **核心状态管理**: 统一管理计时状态、提醒倒计时、当前标签。
    *   **标签分类**: 区分 `displayTags` (首页展示) 和 `otherTags`。
    *   **生命周期感知**: 处理应用前后台切换时的时间差计算。

### 3. 数据层 (`data/`)

*   **Repositories**:
    *   [`TimeRecordRepository.kt`](app/src/main/java/com/example/timemanager/data/TimeRecordRepository.kt): 管理时间记录，使用 `time_records.json` 进行本地存储。
    *   [`HealthRecordRepository.kt`](app/src/main/java/com/example/timemanager/data/HealthRecordRepository.kt): 管理健康/提醒打卡记录，使用 `health_records.json` 存储。
    *   [`TagRepository.kt`](app/src/main/java/com/example/timemanager/data/TagRepository.kt): 管理标签数据，支持 `showOnHome` 字段持久化。

*   **Models**:
    *   `Tag`: 包含 `name`, `color`, `showOnHome`。
    *   `Task`, `TimeRecord`, `HealthRecord`, `TimerState`.

### 4. 服务 (`service/`)

*   [`NotificationService.kt`](app/src/main/java/com/example/timemanager/service/NotificationService.kt):
    *   处理系统通知发送（如提醒时间到）。

## 技术栈 (Tech Stack)

*   **语言**: Kotlin
*   **UI 框架**: Jetpack Compose (Material3)
*   **架构模式**: MVVM (Model-View-ViewModel)
*   **数据存储**:
    *   JSON 文件存储 (Complex Data)
    *   SharedPreferences (Settings & Tags)
*   **构建工具**: Gradle Kotlin DSL
