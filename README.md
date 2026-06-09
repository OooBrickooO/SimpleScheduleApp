# SimpleSchedule (极简课表)

一款基于 Android 原生 (Kotlin + Jetpack Compose) 开发的高颜值、极简几何风课表软件。支持复杂排课、多时间表管理、深度的系统级集成（前台服务提醒、精准闹钟、桌面组件）以及教务系统一键导入。

>    分发网站：[www.lingflame.cn](https://www.lingflame.cn) (支持在线下载最新 APK 与检查更新)
>    本项目由 AI 辅助，完全单人独立开发模式进行构建与迭代。

---

## 🌟 项目特色

*   **极简高颜值**：遵循 Material You / 动态壁纸配色规范 (Android 12+)，支持亮暗模式切换及多彩课表格子自适应，带给您极简且优雅的视觉体验。
*   **现代化架构**：基于 Jetpack Compose 构建纯声明式 UI，底层采用 MVVM + Room 数据库 + DataStore 偏好存储，运行流畅。
*   **智能排课引擎**：
    *   支持单双周、多段不连续周次智能解析与冲突检测。
    *   支持长按课程卡片直接拖拽调课。
    *   非本周课程跨周次透明预显示（支持单双周独立标识）。
*   **系统深度集成**：使用 Jetpack Glance 构建的高性能桌面微件，自适应系统深浅色。
*   **灵活的数据导入**：
    *   支持教务系统（正方等）内嵌 WebView JS 脚本一键无感抓取。 ⭐如果为非新正课表，需要自己适配自己的大学，或在QQ群（817954315）内联系我让我更新⭐
    *   支持 Base64 编码的课表口令分享与解析导入。
*   **自动检查更新**：内置客户端在线检测更新服务，利用系统 `DownloadManager` 自动下载并调起安全覆盖安装。

---

## 🛠️ 技术栈与架构

*   **开发语言**：Kotlin
*   **UI 框架**：Jetpack Compose / Jetpack Glance (Widgets)
*   **数据存储**：Room Database / DataStore Preferences
*   **异步编程**：Kotlin Coroutines & Flow
*   **后端服务**：PHP (统计与跳转) & Linux Nginx 静态托管
*   **部署脚本**：PowerShell (一键云端自动部署，支持多路径 APK 自动检测及免密上传)

---

## 🚀 编译与运行

1.  确保已安装 Android Studio (Giraffe 或更高版本)。
2.  确保 JDK 版本为 Java 21（或 Java 11+）。
3.  克隆本项目到本地：
    ```bash
    git clone https://github.com/OooBrickooO/SimpleScheduleApp.git
    ```
4.  使用 Android Studio 打开项目，等待 Gradle 同步完成。
5.  点击 Run 部署到手机或模拟器（要求 Android 7.0 / API 24 以上，最佳体验建议 Android 12 以上）。

---

## 📦 部署与分发

项目包含了专为开发者打造的自动化部署流程：
*   **一键云部署**：运行 `.\deploy.ps1` 即可将最新打包的 `app-release.apk` 以及计数器脚本一键上传部署到阿里云服务器。

---

## 📄 许可证 (License)

本项目采用 Apache License 2.0 开源协议。
