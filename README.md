# TimeManager Project Documentation

## 简介
TimeManager 是一款 Android 时间管理应用，基于 Jetpack Compose 开发。它摒弃了传统的后台即时计时模式，采用更优雅的“起止点”记录方案，结合沉浸式的沙漏动画与横屏时钟体验，帮助用户轻松追踪时间。同时内置喝水与久坐提醒功能，关注用户健康。

## 重大版本重构与实施规划 (Major Refactoring & Implementation Plan)

### 前端修改要求 (Frontend Requirements)
1. **主页面改造**
   - **顶部**: 保留标题栏。
   - **中间**: 设置圆形开始按钮。
     - 需实现长按动画效果：外圈顺时针逐渐填充。
     - 长按完成后进入设置状态（选择标签等）。
   - **状态切换**: 开始后按钮消失，动画生成上下两个新按钮：
     - 上方：专注按钮（进入横屏页面）。
     - 下方：结束按钮。
   - **健康图标**: 开始后动态生成饮水和活动图标。
     - 样式：长条形（类似温度计）。
     - 效果：随时间颜色淡化。
     - 交互：点击可恢复图标颜色。
   - **入口**: 右下角设置历史记录和设置的圆形入口。

2. **历史记录页面改造**
   - **时间轴**: 采用 24 小时时钟样式。
   - **标尺**: 显示 0-24 小时刻度。
   - **显示**: 记录时间段以填充方式显示。
   - **交互**: 空白区域点击功能。
     - 自动选取无记录时间段的头尾作为记录时间。
     - 弹出弹窗手动选择标签和输入描述。

### 后端修改要求 (Backend Requirements)
*(注：此处后端指代应用内部数据层与逻辑层，或将来扩展的服务端)*

1. **开发新的 API 接口 (Internal/External APIs)**
   - 长按开始状态记录接口。
   - 专注/结束状态切换接口。
   - 饮水和活动状态记录接口。
   - 历史记录时间轴数据接口。
   - 快速记录创建接口。

2. **数据模型调整 (Data Model)**
   - 新增长按开始记录字段。
   - 修改历史记录时间存储格式。
   - 增加快速记录标记字段。

### 实施规划 (Implementation Plan)

#### 【后端部分 - Coder 任务】
1. **第一阶段：API 开发**
   - [ ] 开发长按开始记录 API（1天）
   - [ ] 实现状态切换 API（0.5天）
   - [ ] 创建饮水和活动记录 API（1天）
   - [ ] 开发历史记录时间轴数据 API（1.5天）
   - [ ] 实现快速记录创建 API（1天）

2. **第二阶段：数据库调整**
   - [ ] 修改历史记录表结构（0.5天）
   - [ ] 新增快速记录字段（0.5天）
   - [ ] 数据迁移脚本开发（1天）

#### 【前端部分 - Builder 任务】
1. **第一阶段：主页面重构**
   - [ ] 实现圆形按钮动画组件（2天）
   - [ ] 开发长按交互逻辑（1天）
   - [ ] 创建状态切换 UI（1天）
   - [ ] 实现温度计式图标组件（2天）

2. **第二阶段：历史页面重构**
   - [ ] 开发 24 小时时钟时间轴（3天）
   - [ ] 实现空白区域点击功能（2天）
   - [ ] 创建快速记录弹窗（1天）

3. **第三阶段：联调测试**
   - [ ] API 接口联调（2天）
   - [ ] 动画效果优化（1天）
   - [ ] 响应式布局测试（1天）

## 当前功能 (Current Features)

1.  **沙漏时间流 (Hourglass Flow)**
    *   **动态交互**: 主界面中央展示动态沙漏，可视化时间的流逝。
    *   **起止点记录**: 点击“开始”记录时间起点，点击“结束”记录时间终点并生成记录。无需后台常驻，省电且精准。
    *   **翻转特效**: 任务结束时沙漏自动翻转，为下一次专注做好准备。

2.  **健康生活提醒 (Health Reminders)**
    *   **独立循环**: 内置“喝水”和“久坐”两个独立循环提醒。
    *   **可视化进度**: 通过圆形水波/能量球图标展示剩余时间。
    *   **灵活重置**: 点击图标即可重置提醒计时（如喝完水后点击）。
    *   **通知触达**: 时间耗尽时发送系统通知提醒用户行动。
    *   **自定义间隔**: 支持在设置中自定义喝水（默认30分钟）和久坐（默认50分钟）的提醒周期。

3.  **专注模式 (Focus Mode)**
    *   **沉浸体验**: 横屏全屏显示，大字号时钟与日期。
    *   **任务看板**: 清晰展示当前进行的任务标签与描述。
    *   **状态同步**: 实时显示健康提醒的剩余进度，无需退出专注模式即可查看。

4.  **标签系统 (Tag System)**
    *   支持自定义标签与颜色。
    *   网格化标签选择器，操作更便捷。

5.  **历史记录 (History)**
    *   **按日查看**: 左右滑动切换日期查看每日记录。
    *   **可视化展示**: 记录块采用标签颜色背景，直观区分任务类型。
    *   **智能排序**: 记录按从早到晚顺序排列，还原一日时间线。

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

*   **Language**: Kotlin
*   **UI**: Jetpack Compose (Canvas Animation, Material3)
*   **Architecture**: MVVM
*   **Storage**: SharedPreferences (State/Config), JSON (Records)
