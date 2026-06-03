SimpleSchedule (极简课表)

一款基于 Android 原生 (Kotlin + Jetpack Compose) 开发的高颜值、极简几何风课表软件。支持复杂排课、多时间表管理、深度的系统级集成（前台服务提醒、精准闹钟、桌面组件）以及教务系统一键导入。

现代化架构：基于 Jetpack Compose 构建纯声明式 UI，底层采用 MVVM + Room 数据库 + DataStore 偏好存储。

智能排课引擎：

支持单双周、多段不连续周次智能解析与冲突检测。

支持长按课程卡片直接拖拽调课。

非本周课程跨周次透明预显示（支持单双周独立标识）。

桌面小部件：使用 Jetpack Glance 构建的高性能桌面微件，自适应深色模式。

灵活的数据导入：

支持教务系统（正方等）内嵌 WebView JS 脚本一键无感抓取。  ⭐如果为非新正课表，需要自己适配自己的大学，或提交issue让我更新⭐

支持 Base64 编码的课表口令分享与解析导入。

🚀 编译与运行

确保已安装 Android Studio (Giraffe 或更高版本)。

确保 JDK 版本为 Java 11 或更高。

克隆本项目到本地：

git clone [https://github.com/你的用户名/SimpleSchedule.git](https://github.com/你的用户名/SimpleSchedule.git)


使用 Android Studio 打开项目，等待 Gradle 同步完成。

点击 Run 部署到手机或模拟器（要求 Android 7.0 / API 24 以上，最佳体验建议 Android 12 以上）。

📄 许可证 (License)

本项目采用 Apache License 2.0 开源协议。
