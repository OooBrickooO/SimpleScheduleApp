package com.example.simpleschedule.domain.parsers

import android.webkit.WebView

/**
 * JavaScript injection scripts for extracting Zhengfang courses/exams and improving mobile rendering.
 */
object ZhengfangJsScripts {

    val UNIVERSAL_MOBILE_FIX_JS: String = """
        javascript:(function() {
            function applyUniversalFixes() {
                try {
                    var meta = document.querySelector('meta[name="viewport"]');
                    if (!meta) {
                        meta = document.createElement('meta');
                        meta.name = 'viewport';
                        document.head && document.head.appendChild(meta);
                    }
                    if (meta) {
                        meta.content = 'width=device-width, initial-scale=1.0, minimum-scale=0.5, maximum-scale=3.0, user-scalable=yes';
                    }

                    var styleId = '__simple_schedule_web_fix__';
                    var styleEl = document.getElementById(styleId);
                    if (!styleEl && document.head) {
                        styleEl = document.createElement('style');
                        styleEl.id = styleId;
                        styleEl.innerHTML = `
                            html, body {
                                overflow: auto !important;
                                overflow-x: auto !important;
                                overflow-y: auto !important;
                                height: auto !important;
                                min-height: 100% !important;
                                max-height: none !important;
                            }
                            .login-page, .login-bg, .login_bg, #app, .wrapper, .main-container,
                            div[class*="login-page"], div[class*="login_bg"], div[class*="wrapper"] {
                                overflow: auto !important;
                                height: auto !important;
                                min-height: 100vh !important;
                                max-height: none !important;
                                padding-bottom: 20px !important;
                            }
                            @media screen and (max-width: 800px) {
                                .login-container, .login-box, .login_box, .login-card, .login-wrapper, .login-wrap,
                                #login-box, #login_box, #loginBox, .auth-box, .auth_box, .login_form, .login-form-wrap,
                                div[class*="login-container"], div[class*="login-box"], div[class*="loginBox"],
                                div[class*="auth-container"], div[class*="login_container"] {
                                    position: relative !important;
                                    top: 10px !important;
                                    right: auto !important;
                                    left: auto !important;
                                    bottom: auto !important;
                                    margin: 10px auto !important;
                                    float: none !important;
                                    max-width: 96% !important;
                                    max-height: none !important;
                                    width: auto !important;
                                    height: auto !important;
                                }
                                #qcodepc, #qcode, .qrcode-bg, #qrcode-img, .qr_code, .qr-code {
                                    position: relative !important;
                                    top: auto !important;
                                    left: auto !important;
                                    margin: 10px auto !important;
                                    float: none !important;
                                    max-width: 100% !important;
                                }
                            }
                        `;
                        document.head.appendChild(styleEl);
                    }

                    var loginInputs = document.querySelectorAll('input[type="password"], input[name*="password"], input[name*="user"], button[type="submit"], #dl, .login-btn');
                    loginInputs.forEach(function(input) {
                        var curr = input.parentElement;
                        while (curr && curr !== document.body && curr !== document.documentElement) {
                            var compStyle = window.getComputedStyle ? window.getComputedStyle(curr) : curr.style;
                            if (compStyle) {
                                if (compStyle.overflow === 'hidden') {
                                    curr.style.setProperty('overflow', 'auto', 'important');
                                }
                                if (window.innerWidth <= 800 && (compStyle.position === 'absolute' || compStyle.position === 'fixed')) {
                                    if (curr.classList.contains('login-container') || curr.classList.contains('login-box') || curr.classList.contains('login_box') || curr.classList.contains('login-card') || (curr.id && curr.id.indexOf('login') !== -1)) {
                                        curr.style.setProperty('position', 'relative', 'important');
                                        curr.style.setProperty('top', '10px', 'important');
                                        curr.style.setProperty('right', 'auto', 'important');
                                        curr.style.setProperty('left', 'auto', 'important');
                                        curr.style.setProperty('margin', '10px auto', 'important');
                                    }
                                }
                            }
                            curr = curr.parentElement;
                        }
                    });
                } catch(e) {
                    console.error('SimpleSchedule web fix error:', e);
                }
            }

            applyUniversalFixes();
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', applyUniversalFixes);
            }
            window.addEventListener('load', applyUniversalFixes);
            setTimeout(applyUniversalFixes, 500);
            setTimeout(applyUniversalFixes, 1500);
        })();
    """.trimIndent()

    val LEGACY_DESKTOP_WIDTH_JS: String = """
        (function() {
            try {
                var styleId = '__simple_schedule_legacy_width__';
                if (!document.getElementById(styleId) && document.head) {
                    var style = document.createElement('style');
                    style.id = styleId;
                    style.textContent =
                        'html, body { min-width: 1100px !important; }' +
                        '.login { min-width: 1100px !important; width: 1100px !important; }';
                    document.head.appendChild(style);
                }
            } catch (e) {
                console.error('SimpleSchedule legacy width error: ' + e);
            }
        })();
    """.trimIndent()

    fun executeZhengfangImportJs(loadUrl: String, webViewRef: WebView?, syncCredits: Boolean) {
        val jsCode = if (loadUrl.contains("lzjtu")) {
            """
            javascript:(function() {
                try {
                    var courses = [];
                    var colorList = ['blue', 'pink', 'purple', 'slate', 'indigo', 'rose'];
                    var nodes = document.querySelectorAll('.kbcontent, .kbcontent1');

                    if (nodes.length === 0) {
                        window.Android.passData("ERROR:未找到课程节点，请确保已进入课表页面！");
                        return;
                    }

                    nodes.forEach(function(node) {
                        var idStr = node.id;
                        if (!idStr) return;
                        var parts = idStr.split('-');
                        if (parts.length < 3) return;

                        var nameNode = node.querySelector('a');
                        if (!nameNode) return;
                        var name = nameNode.innerText.trim();
                        if (!name) return;

                        var teacherNode = node.querySelector('font[title="教师"]');
                        var teacher = teacherNode ? teacherNode.innerText.trim() : '';

                        var locationNode = node.querySelector('font[title="教室"]');
                        var location = locationNode ? locationNode.innerText.trim() : '未排地点';

                        var timeNode = node.querySelector('font[title="周次(节次)"]');
                        if (!timeNode) return;
                        var timeText = timeNode.innerText.trim();

                        var weekStr = timeText.split('(周)')[0];
                        var sectionStr = timeText.match(/\[(\d+)-(\d+)节\]/);
                        var startNode = 1, endNode = 2;
                        if (sectionStr) {
                            startNode = parseInt(sectionStr[1], 10);
                            endNode = parseInt(sectionStr[2], 10);
                        } else {
                            var tr = node.closest('tr');
                            if(tr) {
                                var th = tr.querySelector('th');
                                if(th && th.innerText.match(/\((\d+),(\d+)小节\)/)) {
                                    var m = th.innerText.match(/\((\d+),(\d+)小节\)/);
                                    startNode = parseInt(m[1], 10);
                                    endNode = parseInt(m[2], 10);
                                }
                            }
                        }

                        var dayOfWeek = parseInt(parts[1], 10);

                        var weeksArr = [];
                        var weekParts = weekStr.split(',');
                        weekParts.forEach(function(p) {
                            var rangeMatch = p.match(/(\d+)-(\d+)/);
                            if (rangeMatch) {
                                for(var i = parseInt(rangeMatch[1], 10); i <= parseInt(rangeMatch[2], 10); i++) {
                                    weeksArr.push(i);
                                }
                            } else {
                                var singleMatch = p.match(/(\d+)/);
                                if (singleMatch) {
                                    weeksArr.push(parseInt(singleMatch[1], 10));
                                }
                            }
                        });

                        if (weeksArr.length === 0) return;

                        var nameHash = 0;
                        for(var i=0; i<name.length; i++) nameHash += name.charCodeAt(i);
                        var colorTheme = colorList[nameHash % colorList.length];

                        var credits = "";
                        if ($syncCredits) {
                            var creditsNode = node.querySelector('font[title="学分"]');
                            credits = creditsNode ? creditsNode.innerText.trim() : '';
                        }

                        courses.push({
                            name: name,
                            location: location,
                            teacher: teacher,
                            dayOfWeek: dayOfWeek,
                            startNode: startNode,
                            endNode: endNode,
                            weeks: JSON.stringify(weeksArr),
                            colorTheme: colorTheme,
                            credits: credits
                        });
                    });

                    var uniqueCourses = [];
                    var seen = new Set();
                    courses.forEach(function(c) {
                        var key = c.name + c.dayOfWeek + c.startNode + c.endNode + c.weeks;
                        if(!seen.has(key)) {
                            seen.add(key);
                            uniqueCourses.push(c);
                        }
                    });

                    window.Android.passData(JSON.stringify(uniqueCourses));
                } catch (e) {
                    window.Android.passData("ERROR:提取异常:" + e.message);
                }
            })();
            """.trimIndent()
        } else {
            """
            javascript:(function() {
                try {
                    var courses = [];
                    var colorList = ['blue', 'pink', 'purple', 'slate', 'indigo', 'rose'];
                    var nodes = document.querySelectorAll('.kbcontent, .timetable_con');

                    if (nodes.length === 0) {
                        window.Android.passData("ERROR:未找到课程节点，请确保已进入「个人课表查询」页面且已显示课表！");
                        return;
                    }

                    nodes.forEach(function(node) {
                        var td = node.closest('td');
                        if (!td || !td.id) return;
                        var dayOfWeek = parseInt(td.id.split('-')[0]);

                        var htmlBlocks = node.innerHTML.split(/<hr[^>]*>/i);

                        htmlBlocks.forEach(function(html) {
                            var tempDiv = document.createElement('div');
                            tempDiv.innerHTML = html;

                            var textContent = tempDiv.innerText.trim();
                            if (!textContent) return;

                            var titleNode = tempDiv.querySelector('.title font, font[title="课程"], font.color-font');
                            var name = titleNode ? titleNode.innerText.replace(/[★○●◇]/g, '').trim() : '';
                            if (!name) {
                                var lines = textContent.split('\n').filter(l => l.trim() !== '');
                                if(lines.length > 0) name = lines[0].replace(/[★○●◇]/g, '').trim();
                            }

                            var timeText = '', location = '未排地点', teacher = '';
                            var credits = '';
                            if ($syncCredits) {
                                var creditsNode = tempDiv.querySelector('font[title="学分"], [title="学分"]');
                                credits = creditsNode ? creditsNode.innerText.trim() : '';
                                if (!credits) {
                                    var xfMatch = textContent.match(/学分[:：]\s*([0-9.]+)/) || textContent.match(/([0-9.]+)\s*学分/);
                                    if (xfMatch) credits = xfMatch[1];
                                }
                            }

                            var ps = tempDiv.querySelectorAll('p, font, span');
                            if (ps.length > 0) {
                                ps.forEach(function(p) {
                                    var text = p.innerText.trim();
                                    if (text.includes('节/周') || text.includes('节)') || text.match(/\d+-\d+节/)) timeText = text;
                                    if (text.includes('校区') || text.includes('楼') || text.includes('教室') || p.getAttribute('title') === '教室') location = text;
                                    if ($syncCredits && (p.getAttribute('title') === '学分' || text.includes('学分'))) {
                                        credits = text.replace('学分：', '').replace('学分', '').trim();
                                    }
                                    if (p.getAttribute('title') === '老师' || text.match(/[\u4e00-\u9fa5]{2,4}/)) {
                                        if(!teacher && text !== name && !text.includes('周') && !text.includes('楼') && !text.includes('学分') && !text.match(/^([0-9](\.[0-9])?)$/)) {
                                            teacher = text;
                                        }
                                    }
                                });
                            } else {
                                var lines = textContent.split('\n').map(l => l.trim()).filter(l => l !== '');
                                lines.forEach(function(line) {
                                    if (line.match(/\d+-\d+节/)) timeText = line;
                                    else if (line.match(/楼|教室|校区/)) location = line;
                                    else if ($syncCredits && line.match(/^([0-9](\.[0-9])?)$/) && !credits) credits = line;
                                });
                            }

                            var startNode = 1, endNode = 2;
                            var weeksArr = [];

                            var timeMatch = timeText.match(/\((\d+)-(\d+)节\)(.*)/) || timeText.match(/(\d+)-(\d+)节(.*)/);
                            if (timeMatch) {
                                startNode = parseInt(timeMatch[1]);
                                endNode = parseInt(timeMatch[2]);
                                var weekStr = timeMatch[3].replace(/\s+/g, '');

                                var parts = weekStr.split(/[,，、]+/);
                                parts.forEach(function(p) {
                                    var isOdd = p.indexOf('单') !== -1;
                                    var isEven = p.indexOf('双') !== -1;
                                    var rangeMatch = p.match(/(\d+)-(\d+)/);
                                    if (rangeMatch) {
                                        for(var i = parseInt(rangeMatch[1]); i <= parseInt(rangeMatch[2]); i++) {
                                            if (isOdd && i % 2 === 0) continue;
                                            if (isEven && i % 2 !== 0) continue;
                                            weeksArr.push(i);
                                        }
                                    } else {
                                        var singleMatch = p.match(/(\d+)/);
                                        if (singleMatch) {
                                            var wNum = parseInt(singleMatch[1]);
                                            if (isOdd && wNum % 2 === 0) return;
                                            if (isEven && wNum % 2 !== 0) return;
                                            weeksArr.push(wNum);
                                        }
                                    }
                                });
                            }

                            if (!name || weeksArr.length === 0) return;

                            var nameHash = 0;
                            for(var i=0; i<name.length; i++) nameHash += name.charCodeAt(i);
                            var colorTheme = colorList[nameHash % colorList.length];

                            courses.push({
                                name: name,
                                location: location.replace('上课地点：', '').trim(),
                                teacher: teacher.replace('教师：', '').trim(),
                                dayOfWeek: dayOfWeek,
                                startNode: startNode,
                                endNode: endNode,
                                weeks: JSON.stringify(weeksArr),
                                colorTheme: colorTheme,
                                credits: credits
                            });
                        });
                    });

                    var uniqueCourses = [];
                    var seen = new Set();
                    courses.forEach(function(c) {
                        var key = c.name + c.dayOfWeek + c.startNode + c.endNode + c.weeks;
                        if(!seen.has(key)) {
                            seen.add(key);
                            uniqueCourses.push(c);
                        }
                    });

                    window.Android.passData(JSON.stringify(uniqueCourses));
                } catch (e) {
                    window.Android.passData("ERROR:提取异常:" + e.message);
                }
            })();
            """.trimIndent()
        }
        webViewRef?.evaluateJavascript(jsCode, null)
    }

    fun executeZhengfangExamJs(webViewRef: WebView?) {
        val jsCode = """
        javascript:(function() {
            try {
                var grid = document.getElementById("tabGrid") || document.querySelector("table.ui-jqgrid-btable");
                if (!grid) {
                    window.Android.passData("ERROR:未找到考试数据表格，请确保已进入「考试信息查询」页面且已加载完成！");
                    return;
                }
                window.Android.passExamData(grid.outerHTML);
            } catch (e) {
                window.Android.passData("ERROR:提取异常:" + e.message);
            }
        })();
        """.trimIndent()
        webViewRef?.evaluateJavascript(jsCode, null)
    }
}
