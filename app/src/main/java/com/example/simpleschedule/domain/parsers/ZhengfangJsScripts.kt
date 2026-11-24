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

    internal fun buildZhengfangImportJs(loadUrl: String, syncCredits: Boolean): String {
        return if (loadUrl.contains("lzjtu")) {
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
                        var name = nameNode.innerText.replace(/[★○●◇☆◆▲△■□※]/g, '').trim();
                        if (!name) return;

                        var teacherNode = node.querySelector('font[title="教师"], font[title="老师"], font[title="任课教师"], font[title="导师"], span[title="教师"], span[title="老师"]');
                        var teacher = teacherNode ? teacherNode.innerText.trim() : '';
                        if (!teacher) {
                            var fallbackLines = (node.innerText || '').split('\n').map(function(l) { return l.trim(); }).filter(function(l) { return l !== ''; });
                            for (var fl = 0; fl < fallbackLines.length; fl++) {
                                var tm = fallbackLines[fl].match(/^(?:教师|老师|导师|任课教师|授课教师)\s*[:：]\s*(.+)$/);
                                if (tm && tm[1]) {
                                    teacher = tm[1].trim();
                                    break;
                                }
                            }
                        }
                        teacher = teacher.replace(/^(?:教师|老师|导师|任课教师|授课教师)\s*[:：]\s*/, '').trim();

                        var locationNode = node.querySelector('font[title="教室"], font[title="上课地点"], font[title="地点"], span[title="教室"], span[title="上课地点"]');
                        var location = locationNode ? locationNode.innerText.trim() : '未排地点';
                        location = location.replace(/^(?:上课地点|教学场地|地点|教室|场地)\s*[:：]\s*/, '').trim() || '未排地点';

                        var timeNode = node.querySelector('font[title="周次(节次)"], font[title="周次"], span[title="周次(节次)"]');
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
                            if (tr) {
                                var th = tr.querySelector('th');
                                if (th && th.innerText.match(/\((\d+),(\d+)小节\)/)) {
                                    var m = th.innerText.match(/\((\d+),(\d+)小节\)/);
                                    startNode = parseInt(m[1], 10);
                                    endNode = parseInt(m[2], 10);
                                }
                            }
                        }

                        var dayOfWeek = parseInt(parts[1], 10);

                        var weeksArr = [];
                        var weekParts = weekStr.split(/[,，、]+/);
                        weekParts.forEach(function(p) {
                            var isOdd = p.indexOf('单') !== -1 || timeText.indexOf('单') !== -1;
                            var isEven = p.indexOf('双') !== -1 || timeText.indexOf('双') !== -1;
                            var rangeMatch = p.match(/(\d+)-(\d+)/);
                            if (rangeMatch) {
                                for (var i = parseInt(rangeMatch[1], 10); i <= parseInt(rangeMatch[2], 10); i++) {
                                    if (isOdd && i % 2 === 0) continue;
                                    if (isEven && i % 2 !== 0) continue;
                                    weeksArr.push(i);
                                }
                            } else {
                                var singleMatch = p.match(/(\d+)/);
                                if (singleMatch) {
                                    var wNum = parseInt(singleMatch[1], 10);
                                    if (isOdd && wNum % 2 === 0) return;
                                    if (isEven && wNum % 2 !== 0) return;
                                    weeksArr.push(wNum);
                                }
                            }
                        });

                        if (weeksArr.length === 0) return;

                        var nameHash = 0;
                        for (var i = 0; i < name.length; i++) nameHash += name.charCodeAt(i);
                        var colorTheme = colorList[nameHash % colorList.length];

                        var credits = "";
                        if ($syncCredits) {
                            var creditsNode = node.querySelector('font[title="学分"], [title="学分"]');
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
                        if (!seen.has(key)) {
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

                    function normalizeText(s) {
                        return (s || '').replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim();
                    }

                    function stripSymbols(s) {
                        return normalizeText(s).replace(/[★○●◇☆◆▲△■□※*]/g, '').trim();
                    }

                    function extractRows(tempDiv) {
                        var rows = [];
                        var rowEls = Array.prototype.slice.call(tempDiv.querySelectorAll('p, div'));
                        var leafEls = rowEls.filter(function(el) {
                            return !el.querySelector('p, div');
                        });

                        function buildRowFromElement(el) {
                            var rawText = typeof el.innerText === 'string' && el.innerText.length > 0
                                ? el.innerText
                                : (el.textContent || '');
                            var text = normalizeText(rawText);
                            if (!text) return null;

                            var titles = [];
                            if (el.getAttribute && el.getAttribute('title')) {
                                titles.push(el.getAttribute('title'));
                            }
                            var titled = el.querySelectorAll('[title]');
                            for (var ti = 0; ti < titled.length; ti++) {
                                var tv = titled[ti].getAttribute('title');
                                if (tv) titles.push(tv);
                            }

                            var classes = [];
                            if (el.className && typeof el.className === 'string') {
                                classes.push(el.className);
                            }
                            var icons = el.querySelectorAll('span, i, font');
                            for (var ii = 0; ii < icons.length; ii++) {
                                if (icons[ii].className && typeof icons[ii].className === 'string') {
                                    classes.push(icons[ii].className);
                                }
                            }

                            return {
                                text: text,
                                titles: titles.join(' '),
                                iconClasses: classes.join(' '),
                                consumed: false
                            };
                        }

                        if (leafEls.length > 0) {
                            leafEls.forEach(function(el) {
                                var row = buildRowFromElement(el);
                                if (row) rows.push(row);
                            });
                        } else {
                            var htmlFragments = tempDiv.innerHTML.split(/<br\s*\/?>|\r?\n/i);
                            htmlFragments.forEach(function(frag) {
                                var fragDiv = document.createElement('div');
                                fragDiv.innerHTML = frag;
                                var row = buildRowFromElement(fragDiv);
                                if (row) rows.push(row);
                            });
                        }
                        return rows;
                    }

                    function isMetadataNoise(text) {
                        if (/^(?:教学班|班级|组成|考核|考核方式|课程性质|性质|总学时|学时|周学时|理论|实践|人数|备注|选课|课程号|课号|编号|校区)\s*[:：]?/.test(text)) {
                            return true;
                        }
                        if (/^(?:必修|选修|限选|任选|公选|通识|专业必修|专业选修|公共必修|公共选修|考查|考试|理论课|实验课|实践课|线下|线上|[0-9]+(?:\.[0-9]+)?)$/.test(text)) {
                            return true;
                        }
                        return false;
                    }

                    function isLikelyLocation(text) {
                        if (/^(?:上课地点|教学场地|授课地点|地点|教室|场地)\s*[:：]/.test(text)) return true;
                        if (/(?:校区|教学楼|实验楼|综合楼|办公楼|科学楼|机电楼|信息楼|楼|教室|实验室|实训室|机房|操场|体育馆|运动场|报告厅|中心|馆|厅|室|场|腾讯会议|雨课堂)/.test(text)) {
                            return true;
                        }
                        if (/[A-Za-z]-?\d{2,4}|\d{3,4}/.test(text) && !/学时|学分|班|周|节/.test(text)) {
                            return true;
                        }
                        return false;
                    }

                    function isLikelyTeacherName(text) {
                        var candidate = normalizeText(text)
                            .replace(/^(?:教师|老师|导师|任课教师|授课教师|指导教师|主讲教师)\s*[:：]\s*/, '')
                            .replace(/[（(\[][^）)\]]{1,12}[）)\]]/g, '')
                            .trim();
                        if (!candidate || candidate.length < 2 || candidate.length > 40) return false;
                        if (/\d/.test(candidate)) return false;
                        if (/(?:周|节|楼|校区|教室|实验室|机房|操场|体育馆|学分|学时|必修|选修|限选|考查|考试|教学班|班级|学院|专业|课程)/.test(candidate)) {
                            return false;
                        }
                        if (/^[\u4e00-\u9fa5]{2,4}(?:[·•][\u4e00-\u9fa5]{1,4})?(?:\s*[,，、\/;；\s]\s*[\u4e00-\u9fa5]{2,4}(?:[·•][\u4e00-\u9fa5]{1,4})?)*$/.test(candidate)) {
                            return true;
                        }
                        if (/^[A-Za-z][A-Za-z.\s'-]{2,39}$/.test(candidate) && candidate.replace(/[^A-Za-z]/g, '').length >= 2) {
                            return true;
                        }
                        return false;
                    }

                    nodes.forEach(function(node) {
                        var td = node.closest('td');
                        if (!td || !td.id) return;
                        var dayOfWeek = parseInt(td.id.split('-')[0], 10);

                        var htmlBlocks = node.innerHTML.split(/<hr[^>]*>/i);

                        htmlBlocks.forEach(function(html) {
                            var tempDiv = document.createElement('div');
                            tempDiv.innerHTML = html;

                            var textContent = normalizeText(tempDiv.innerText || tempDiv.textContent || '');
                            if (!textContent) return;

                            var titleNode = tempDiv.querySelector('.title font, .title, font[title="课程"], [title="课程"], font.color-font');
                            var name = titleNode ? stripSymbols(titleNode.innerText || titleNode.textContent || '') : '';
                            if (!name) {
                                var rawLines = (tempDiv.innerText || tempDiv.textContent || '').split('\n').map(function(l) { return stripSymbols(l); }).filter(function(l) { return l !== ''; });
                                if (rawLines.length > 0) name = rawLines[0];
                            }
                            var normalizedName = stripSymbols(name);
                            var rows = extractRows(tempDiv);

                            var timeText = '', location = '', teacher = '', credits = '';
                            if ($syncCredits) {
                                var creditsNode = tempDiv.querySelector('font[title="学分"], [title="学分"]');
                                credits = creditsNode ? normalizeText(creditsNode.innerText || creditsNode.textContent || '') : '';
                                if (!credits) {
                                    var xfMatch = textContent.match(/学分\s*[:：]?\s*([0-9]+(?:\.[0-9]+)?)/) || textContent.match(/([0-9]+(?:\.[0-9]+)?)\s*学分/);
                                    if (xfMatch) credits = xfMatch[1];
                                }
                            }

                            // Pass 1: Exact DOM attributes, icons, and explicit field prefixes
                            rows.forEach(function(row) {
                                var text = row.text;
                                if (!text || stripSymbols(text) === normalizedName) {
                                    row.consumed = true;
                                    return;
                                }

                                var titleLower = row.titles.toLowerCase();
                                var iconLower = row.iconClasses.toLowerCase();

                                if (/周次|节次|时间/.test(row.titles) ||
                                    /glyphicon-time|icon-time|fa-clock/.test(iconLower) ||
                                    /\(?\d+\s*[-~～]\s*\d+\s*节\)?/.test(text) ||
                                    (text.indexOf('节') !== -1 && text.indexOf('周') !== -1)) {
                                    if (!timeText) timeText = text;
                                    row.consumed = true;
                                    return;
                                }

                                if ($syncCredits && (row.titles.indexOf('学分') !== -1 || /学分/.test(text))) {
                                    var cm = text.match(/([0-9]+(?:\.[0-9]+)?)/);
                                    if (cm && !credits) credits = cm[1];
                                    row.consumed = true;
                                    return;
                                }

                                var teacherPrefixMatch = text.match(/^(?:教师|老师|导师|任课教师|授课教师|指导教师|主讲教师)\s*[:：]\s*(.+)$/);
                                if (/教师|老师|导师|任课|授课|主讲|teacher/.test(titleLower) ||
                                    /glyphicon-user|icon-user|fa-user/.test(iconLower) ||
                                    teacherPrefixMatch) {
                                    var extractedTeacher = teacherPrefixMatch ? normalizeText(teacherPrefixMatch[1]) : text;
                                    if (!teacher && extractedTeacher) {
                                        teacher = extractedTeacher;
                                    }
                                    row.consumed = true;
                                    return;
                                }

                                var locPrefixMatch = text.match(/^(?:上课地点|教学场地|授课地点|地点|教室|场地)\s*[:：]\s*(.+)$/);
                                if (/上课地点|教学场地|教室|地点|场地|location/.test(titleLower) ||
                                    /glyphicon-map-marker|icon-map|fa-map-marker/.test(iconLower) ||
                                    locPrefixMatch) {
                                    var extractedLoc = locPrefixMatch ? normalizeText(locPrefixMatch[1]) : text;
                                    if (!location && extractedLoc) {
                                        location = extractedLoc;
                                    }
                                    row.consumed = true;
                                    return;
                                }
                            });

                            // Pass 2: Fallback heuristics only on unconsumed rows
                            rows.forEach(function(row) {
                                if (row.consumed) return;
                                var text = row.text;
                                if (isMetadataNoise(text)) {
                                    row.consumed = true;
                                    return;
                                }

                                if (!location && isLikelyLocation(text)) {
                                    location = text;
                                    row.consumed = true;
                                    return;
                                }

                                if (!teacher && isLikelyTeacherName(text)) {
                                    teacher = text;
                                    row.consumed = true;
                                    return;
                                }
                            });

                            var startNode = 1, endNode = 2;
                            var weeksArr = [];

                            var sectionMatch = timeText.match(/\(?(\d+)\s*[-~～]\s*(\d+)\s*节\)?/);
                            if (sectionMatch) {
                                startNode = parseInt(sectionMatch[1], 10);
                                endNode = parseInt(sectionMatch[2], 10);

                                var weekStr = timeText.replace(sectionMatch[0], '').replace(/\s+/g, '');
                                var parts = weekStr.split(/[,，、]+/);
                                parts.forEach(function(p) {
                                    if (!p) return;
                                    var isOdd = p.indexOf('单') !== -1;
                                    var isEven = p.indexOf('双') !== -1;
                                    var rangeMatch = p.match(/(\d+)\s*[-~～]\s*(\d+)/);
                                    if (rangeMatch) {
                                        var ws = parseInt(rangeMatch[1], 10);
                                        var we = parseInt(rangeMatch[2], 10);
                                        for (var i = ws; i <= we; i++) {
                                            if (isOdd && i % 2 === 0) continue;
                                            if (isEven && i % 2 !== 0) continue;
                                            weeksArr.push(i);
                                        }
                                    } else {
                                        var singleMatch = p.match(/(\d+)/);
                                        if (singleMatch) {
                                            var wNum = parseInt(singleMatch[1], 10);
                                            if (isOdd && wNum % 2 === 0) return;
                                            if (isEven && wNum % 2 !== 0) return;
                                            weeksArr.push(wNum);
                                        }
                                    }
                                });
                            }

                            if (!name || weeksArr.length === 0) return;

                            var cleanLocation = normalizeText(location.replace(/^(?:上课地点|教学场地|授课地点|地点|教室|场地)\s*[:：]\s*/, '')) || '未排地点';
                            var cleanTeacher = normalizeText(teacher.replace(/^(?:教师|老师|导师|任课教师|授课教师|指导教师|主讲教师)\s*[:：]\s*/, ''));

                            var nameHash = 0;
                            for (var i = 0; i < name.length; i++) nameHash += name.charCodeAt(i);
                            var colorTheme = colorList[nameHash % colorList.length];

                            courses.push({
                                name: name,
                                location: cleanLocation,
                                teacher: cleanTeacher,
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
                        if (!seen.has(key)) {
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
    }

    fun executeZhengfangImportJs(loadUrl: String, webViewRef: WebView?, syncCredits: Boolean) {
        val jsCode = buildZhengfangImportJs(loadUrl, syncCredits)
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
