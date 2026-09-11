# 为课前提醒设置添加展开动画

在“课前提醒”设置页面中，为勾选“启用上课提醒”及“显示通知栏提醒”后出现的二级菜单添加平滑的垂直展开和淡入动画。

## 用户审核要求

> [!IMPORTANT]
> - 当“启用上课提醒”被勾选时，下方的设置项（提前时间、通知设置等）应平滑展开。
> - 当“显示通知栏提醒”被勾选时，其子项（实时通知）也应平滑展开。
> - 使用 Compose 的 `AnimatedVisibility` 实现丝滑的视觉过渡。

## 拟议变更

### UI 动画增强

#### [修改] [AppScreens.kt](file:///E:/Git/SimpleScheduleApp/app/src/main/java/com/example/simpleschedule/ui/screens/AppScreens.kt)
- **ReminderSettingsScreen**：
    - 将 `if (reminderEnabled) { ... }` 块包装在 `AnimatedVisibility` 中。
    - 将嵌套的 `if (reminderNotifyEnabled) { ... }` 块也包装在 `AnimatedVisibility` 中。
    - 配置 `enter = expandVertically() + fadeIn()` 和 `exit = shrinkVertically() + fadeOut()`。

## 验证计划

### 手动验证
- 进入“课前提醒”页面。
- 点击“启用上课提醒”复选框，观察下方菜单是否平滑展开/收起。
- 在已启用的情况下，点击“显示通知栏提醒”，观察“使用实时通知”选项是否平滑展开。
- 确保动画过程中布局没有闪烁或突变。
