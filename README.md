# TimeManager Project Documentation

## 简介
TimeManager 是一款 Android 时间管理应用，基于 Jetpack Compose 开发。它摒弃了传统的后台即时计时模式，采用更优雅的“起止点”记录方案，横屏时钟体验，帮助用户轻松追踪时间。同时内置喝水与久坐提醒功能，关注用户健康。

## 重大版本重构与实施规划 (Major Refactoring & Implementation Plan)

### 前端修改要求 (Frontend Requirements)
1. **设置页 - 标签管理增强 (Settings - Enhanced Tag Management)**
   - [ ] 修改 `OptionsScreen` 中的标签列表项。
   - [ ] 为每个标签添加“首页展示 (Show on Home)”的勾选框/开关。
   - [ ] 确保 UI 能正确反映标签的 `showOnHome` 状态。

2. **主页 - 标签滑动选择器 (Home - Tag Swipe Selector)**
   - [ ] 在 `HomeScreen` 引入 `HorizontalPager` (Jetpack Compose)。
   - [ ] 数据源：所有 `showOnHome=true` 的标签 + 一个固定的“其他 (Other)”选项。
   - [ ] 交互：左右滑动切换当前选中的预设任务（Tag）。
   - [ ] “其他”选项逻辑：当滑动到“其他”时，提供二级选择（如点击弹出下拉框或对话框）来选择剩余的标签。

3. **主页 - 新版开始/结束流程 (Home - New Start/Stop Flow)**
   - [ ] **开始 (Start)**: 修改 `StartButton` 交互。长按 (Long Press) -> 直接以当前 Pager 选中的 Tag 开始计时，无需弹出编辑框。
   - [ ] **结束 (Stop)**: 点击停止 -> 停止计时 -> 立即弹出“记录编辑弹窗 (Record Edit Dialog)”。

4. **记录编辑弹窗 (Record Edit Dialog)**
   - [ ] 创建一个新的 Dialog 组件，用于结束计时后显示。
   - [ ] 显示内容：预设的 Tag（不可变）、开始/结束时间（只读）。
   - [ ] 输入内容：描述 (Description)。
   - [ ] 操作：保存 (Save) 或 丢弃 (Discard)。

### 后端修改要求 (Backend Requirements)
*(注：此处后端指代应用内部数据层与逻辑层，或将来扩展的服务端)*

1. **数据模型升级 (Data Model Update)**
   - [ ] 修改 `Tag` 数据类，增加字段 `val showOnHome: Boolean`。
   - [ ] 更新 `TagRepository`：
     - [ ] 处理新字段的 JSON 读写。
     - [ ] 初始化逻辑：默认将 "Working" 和 "Sleeping" 设为 `showOnHome=true`，其余默认为 `false`。

2. **ViewModel 逻辑重构 (ViewModel Refactoring)**
   - [ ] 在 `TimerViewModel` 中区分 `displayTags` (用于 Pager) 和 `otherTags` (用于二级菜单)。
   - [ ] 维护 `currentSelectedTag` 状态，随 Pager 滑动或“其他”选项的选择而更新。
   - [ ] 重构 `startTimer` 逻辑：支持直接传入 Tag 或使用当前选中的 Tag 启动。
   - [ ] 重构 `stopTimer` 逻辑：停止后触发 UI 事件以显示编辑弹窗。

## 当前功能 (Current Features)

*   **时间追踪 (Time Tracking)**:
    *   通过简单的“开始”和“停止”操作记录任务时长。
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
    *   [`HomeScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/HomeScreen.kt): 主页，集成计时控制与提醒。
    *   [`TimeRecordsScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/TimeRecordsScreen.kt): 历史记录列表。
    *   [`OptionsScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/OptionsScreen.kt): 设置与标签管理。
    *   [`AmbientDisplayScreen.kt`](app/src/main/java/com/example/timemanager/ui/screens/AmbientDisplayScreen.kt): 专注模式 UI 实现。

*   **Components (`ui/components/`)**
    *   [`StartButton.kt`](app/src/main/java/com/example/timemanager/ui/components/StartButton.kt): 计时控制按钮。
    *   [`ThermometerReminder.kt`](app/src/main/java/com/example/timemanager/ui/components/ThermometerReminder.kt): 垂直温度计样式的提醒进度条。
    *   [`ReminderButton.kt`](app/src/main/java/com/example/timemanager/ui/components/ReminderButton.kt): 圆形提醒按钮。
    *   [`TagSelectionDialog.kt`](app/src/main/java/com/example/timemanager/ui/components/TagSelectionDialog.kt): 标签选择对话框。

### 2. 逻辑层 (`viewmodel/`)

*   [`TimerViewModel.kt`](app/src/main/java/com/example/timemanager/viewmodel/TimerViewModel.kt):
    *   **核心状态管理**: 统一管理计时状态、提醒倒计时、当前标签。
    *   **生命周期感知**: 处理应用前后台切换时的时间差计算。

### 3. 数据层 (`data/`)

*   **Repositories**:
    *   [`TimeRecordRepository.kt`](app/src/main/java/com/example/timemanager/data/TimeRecordRepository.kt): 管理时间记录，使用 `time_records.json` 进行本地存储。
    *   [`HealthRecordRepository.kt`](app/src/main/java/com/example/timemanager/data/HealthRecordRepository.kt): 管理健康/提醒打卡记录，使用 `health_records.json` 存储。
    *   [`TagRepository.kt`](app/src/main/java/com/example/timemanager/data/TagRepository.kt): 管理标签数据，使用 `SharedPreferences` 存储。

*   **Models**:
    *   `Task`, `TimeRecord`, `HealthRecord`, `Tag`, `TimerState`.

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
