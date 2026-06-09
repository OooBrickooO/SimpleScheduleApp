<?php
$counterFile = 'download_count.txt';
$count = file_exists($counterFile) ? (int)file_get_contents($counterFile) : 0;
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SimpleSchedule - 优雅的桌面课表助手</title>
    <!-- 引入 Outfit 与 JetBrains Mono 字体 -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;800&family=JetBrains+Mono:wght@400;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-color: #FAFAFA;
            --dot-color: rgba(9, 9, 11, 0.05);
            --card-bg: rgba(255, 255, 255, 0.7);
            --card-border: #E4E4E7;
            --text-main: #09090B;
            --text-muted: #71717A;
            --accent-color: #6366F1;
            --accent-gradient: linear-gradient(135deg, #4F46E5, #818CF8);
            --btn-text: #FAFAFA;
            --shadow-color: rgba(0, 0, 0, 0.05);
            --glow-color: rgba(99, 102, 241, 0.15);
            --matrix-color: #ECECF1;
        }

        [data-theme="dark"] {
            --bg-color: #09090B;
            --dot-color: rgba(250, 250, 250, 0.05);
            --card-bg: rgba(24, 24, 27, 0.7);
            --card-border: #27272A;
            --text-main: #FAFAFA;
            --text-muted: #A1A1AA;
            --accent-color: #A5B4FC;
            --accent-gradient: linear-gradient(135deg, #6366F1, #C7D2FE);
            --btn-text: #09090B;
            --shadow-color: rgba(0, 0, 0, 0.3);
            --glow-color: rgba(165, 180, 252, 0.15);
            --matrix-color: #18181C;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            transition: background-color 0.4s ease, border-color 0.4s ease, color 0.4s ease;
        }

        body {
            font-family: 'Outfit', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background-color: var(--bg-color);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            overflow-x: hidden;
            position: relative;
        }

        /* 点阵背景 (Dot Matrix Background) */
        .background-matrix {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-image: radial-gradient(var(--dot-color) 1.5px, transparent 1.5px);
            background-size: 24px 24px;
            z-index: 0;
            pointer-events: none;
        }

        /* 亮暗切换按钮 */
        .theme-toggle {
            position: fixed;
            top: 24px;
            right: 24px;
            width: 48px;
            height: 48px;
            border-radius: 12px;
            background-color: var(--card-bg);
            border: 1px solid var(--card-border);
            cursor: pointer;
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 10;
            box-shadow: 0 4px 12px var(--shadow-color);
            transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), background-color 0.3s, border-color 0.3s;
        }

        .theme-toggle:hover {
            transform: scale(1.1) rotate(15deg);
        }

        .theme-toggle svg {
            width: 20px;
            height: 20px;
            fill: none;
            stroke: var(--text-main);
            stroke-width: 2;
            stroke-linecap: round;
            stroke-linejoin: round;
        }

        .theme-toggle .sun-icon {
            display: none;
        }

        [data-theme="dark"] .theme-toggle .sun-icon {
            display: block;
        }

        [data-theme="dark"] .theme-toggle .moon-icon {
            display: none;
        }

        /* 核心卡片 (Main Container Card) */
        .main-card {
            background-color: var(--card-bg);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid var(--card-border);
            border-radius: 24px;
            padding: 48px 32px;
            width: 90%;
            max-width: 440px;
            text-align: center;
            box-shadow: 0 20px 40px var(--shadow-color);
            z-index: 1;
            position: relative;
            transform: translateY(20px);
            opacity: 0;
            animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }

        @keyframes slideUp {
            to {
                transform: translateY(0);
                opacity: 1;
            }
        }

        /* Logo 动画 */
        .logo-box {
            width: 64px;
            height: 64px;
            border-radius: 16px;
            background: var(--accent-gradient);
            margin: 0 auto 24px auto;
            display: flex;
            justify-content: center;
            align-items: center;
            font-size: 32px;
            font-weight: 800;
            color: var(--btn-text);
            box-shadow: 0 8px 24px var(--glow-color);
            position: relative;
            animation: float 4s ease-in-out infinite;
        }

        @keyframes float {
            0%, 100% { transform: translateY(0px); }
            50% { transform: translateY(-8px); }
        }

        /* 标题与描述 */
        h1 {
            font-size: 36px;
            font-weight: 800;
            letter-spacing: -0.5px;
            margin-bottom: 12px;
            background: var(--accent-gradient);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }

        .subtitle {
            font-size: 14px;
            color: var(--text-muted);
            line-height: 1.6;
            margin-bottom: 32px;
            max-width: 280px;
            margin-left: auto;
            margin-right: auto;
        }

        /* 预览日程格子（与 App 界面相呼应） */
        .schedule-preview {
            background-color: var(--matrix-color);
            border-radius: 16px;
            padding: 16px;
            margin-bottom: 32px;
            display: flex;
            flex-direction: column;
            gap: 8px;
            position: relative;
            overflow: hidden;
            border: 0.5px solid var(--card-border);
        }

        .preview-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 11px;
            font-weight: 600;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 4px;
        }

        .course-card-dummy {
            background-color: rgba(99, 102, 241, 0.1);
            border: 1px dashed rgba(99, 102, 241, 0.3);
            border-radius: 10px;
            padding: 12px;
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            text-align: left;
            position: relative;
            overflow: hidden;
        }

        .course-card-dummy::before {
            content: '';
            position: absolute;
            left: 0;
            top: 0;
            height: 100%;
            width: 4px;
            background: var(--accent-gradient);
        }

        .course-title {
            font-size: 13px;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 4px;
        }

        .course-info {
            font-size: 10px;
            color: var(--text-muted);
        }

        /* 统计数据 */
        .stat-box {
            margin-bottom: 24px;
        }

        .stat-value {
            font-family: 'JetBrains Mono', monospace;
            font-size: 28px;
            font-weight: 700;
            color: var(--text-main);
            display: inline-block;
            position: relative;
        }

        .stat-label {
            font-size: 11px;
            font-weight: 600;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-top: 4px;
        }

        /* 按钮设计 */
        .download-btn {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 10px;
            width: 100%;
            height: 56px;
            border-radius: 14px;
            background: var(--accent-gradient);
            color: var(--btn-text);
            font-size: 16px;
            font-weight: 700;
            text-decoration: none;
            box-shadow: 0 8px 24px var(--glow-color);
            transition: transform 0.2s ease, box-shadow 0.2s ease;
            position: relative;
            overflow: hidden;
        }

        .download-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 12px 32px var(--glow-color);
        }

        .download-btn:active {
            transform: translateY(1px);
        }

        .download-btn svg {
            width: 20px;
            height: 20px;
            fill: none;
            stroke: currentColor;
            stroke-width: 2.5;
            stroke-linecap: round;
            stroke-linejoin: round;
        }

        /* 页脚 */
        footer {
            margin-top: 24px;
            font-size: 11px;
            color: var(--text-muted);
            z-index: 1;
        }

        footer a {
            color: var(--text-muted);
            text-decoration: none;
            font-weight: 600;
        }

        footer a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>

    <div class="background-matrix"></div>

    <!-- 亮暗模式切换 -->
    <button class="theme-toggle" id="theme-btn" aria-label="切换主题">
        <!-- 月亮图标 (暗模式下显示) -->
        <svg class="moon-icon" viewBox="0 0 24 24">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
        </svg>
        <!-- 太阳图标 (亮模式下显示) -->
        <svg class="sun-icon" viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="5"></circle>
            <line x1="12" y1="1" x2="12" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="23"></line>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
            <line x1="1" y1="12" x2="3" y2="12"></line>
            <line x1="21" y1="12" x2="23" y2="12"></line>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
        </svg>
    </button>

    <!-- 核心卡片 -->
    <main class="main-card">
        <div class="logo-box">S</div>
        <h1>SimpleSchedule</h1>
        <p class="subtitle">优雅、快捷、高颜值的安卓桌面课表助手，完美适配壁纸动态配色</p>

        <!-- 课表风格卡片预览 -->
        <div class="schedule-preview">
            <div class="preview-header">
                <span>Today's Schedule</span>
                <span>Week 1</span>
            </div>
            <div class="course-card-dummy">
                <span class="course-title">移动软件开发实习</span>
                <span class="course-info">📍 翔宇楼 302 | ⏰ 14:00 - 15:35</span>
            </div>
        </div>

        <!-- 统计 -->
        <div class="stat-box">
            <div class="stat-value" id="download-count"><?php echo $count; ?></div>
            <div class="stat-label">Total Downloads</div>
        </div>

        <!-- 下载按钮 -->
        <a href="https://www.lingflame.cn/download.php" class="download-btn" id="download-link">
            <svg viewBox="0 0 24 24">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            下载 APK 安装包 (v2.1.0.0609)
        </a>
    </main>

    <footer>
        Designed for <a href="https://github.com/OooBrickooO/SimpleScheduleApp" target="_blank">SimpleSchedule</a> • © 2026
    </footer>

    <script>
        const themeBtn = document.getElementById('theme-btn');
        const body = document.documentElement;

        // 初始化主题
        const savedTheme = localStorage.getItem('theme');
        const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        
        if (savedTheme === 'dark' || (!savedTheme && systemPrefersDark)) {
            body.setAttribute('data-theme', 'dark');
        } else {
            body.setAttribute('data-theme', 'light');
        }

        // 切换主题逻辑
        themeBtn.addEventListener('click', () => {
            const currentTheme = body.getAttribute('data-theme');
            const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
            body.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
        });

        // 按钮点击微动画与统计自增假体验（点击后瞬间增加数字，给用户优秀的互动反馈）
        const downloadLink = document.getElementById('download-link');
        const countSpan = document.getElementById('download-count');
        
        downloadLink.addEventListener('click', () => {
            // 点击后微微震动并数字+1
            setTimeout(() => {
                let currentVal = parseInt(countSpan.innerText);
                countSpan.innerText = currentVal + 1;
                countSpan.style.transform = 'scale(1.2)';
                countSpan.style.transition = 'transform 0.1s ease';
                setTimeout(() => {
                    countSpan.style.transform = 'scale(1)';
                }, 150);
            }, 300);
        });
    </script>
</body>
</html>
