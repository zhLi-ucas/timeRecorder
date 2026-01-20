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

这里说明已有的项目功能

## 项目结构说明 (Project Structure)

项目源代码位于 `app/src/main/java/com/example/timemanager/`，采用 MVVM 架构。

### 1. UI 层 (`ui/`)

*   **Components (`ui/components/`)**
    *   [`HourglassDisplay.kt`](app/src/main/java/com/example/timemanager/ui/components/HourglassDisplay.kt): 核心组件，使用 Canvas 绘制动态流沙与翻转动画。
    *   [`ReminderButton.kt`](app/src/main/java/com/example/timemanager/ui/components/ReminderButton.kt): 圆形进度按钮，用于健康提醒的展示与交互。
    *   [`TagSelectionDialog.kt`](app/src/main/java/com/example/timemanager/ui/components/TagSelectionDialog.kt): 标签选择弹窗。

*   **Screens (`ui/screens/`)**
    *   [`HomeScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/HomeScreen.kt): 应用主页，集成沙漏、提醒按钮与导航入口。
    *   [`AmbientDisplayScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/AmbientDisplayScreen.kt): 专注模式界面，纯净显示时间与任务信息。
    *   [`OptionsScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/OptionsScreen.kt): 设置页，管理标签与提醒间隔。
    *   [`TimeRecordsScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/TimeRecordsScreen.kt): 历史记录页，支持按日翻页查看。

### 2. 逻辑层 (`viewmodel/`)

*   [`TimerViewModel.kt`](app/src/main/java/com/example/timemanager/viewmodel/TimerViewModel.kt): 
    *   **状态中心**: 管理沙漏状态、健康提醒进度、当前任务信息。
    *   **持久化逻辑**: 利用 SharedPreferences 存储任务开始时间戳 (`ongoing_start_time`)，实现无服务状态恢复。
    *   **提醒循环**: 内部维护协程循环，计算并更新健康提醒的进度状态。

### 3. 数据层 (`data/`)

*   **Models**: `Task` (运行时), `TimeRecord` (持久化), `Tag` (元数据).
*   **Repositories**:
    *   `TimeRecordRepository`: JSON 文件存储历史记录。
    *   `TagRepository`: SharedPreferences 存储标签。
    *   `DurationRepository`: (已废弃，转为 Reminder Settings).

### 4. 服务 (`service/`)

*   [`NotificationService.kt`](app/src/main/java/com/example/timemanager/service/NotificationService.kt): 
    *   管理通知通道 (`Channel`)。
    *   发送“喝水”与“久坐”系统通知。

---

## 技术栈 (Tech Stack)

描述用了语言，技术和存储结构等。