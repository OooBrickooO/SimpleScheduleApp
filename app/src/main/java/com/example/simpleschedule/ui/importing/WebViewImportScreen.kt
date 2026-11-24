package com.example.simpleschedule.ui.importing

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.domain.parsers.SchoolCompatRegistry
import com.example.simpleschedule.domain.parsers.ZhengfangJsScripts
import com.example.simpleschedule.domain.parsers.parseQingGuoXls
import com.example.simpleschedule.ui.common.AppBackHandler
import com.example.simpleschedule.ui.theme.BgDark
import com.example.simpleschedule.ui.theme.BgLight
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.LocalAccentColor
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import com.example.simpleschedule.utils.downloadCsvTemplate
import com.example.simpleschedule.utils.openFileManagerForFile
import com.example.simpleschedule.utils.parseCourseCsv
import com.example.simpleschedule.utils.parseZhengfangExamHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

data class School(
    val name: String,
    val type: String, // "zf" or "qg"
    val initial: String,
    val url: String
)

enum class ImportStage {
    SELECT_MODE,
    WEB_IMPORT,
    XLS_IMPORT,
    CSV_IMPORT
}

private const val CSV_TEMPLATE_URL = "https://www.lingflame.cn/download.php?type=template"
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36"

private fun loadSchoolsFromAssets(context: android.content.Context): List<School> {
    return try {
        val jsonStr = context.assets.open("schools.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonStr)
        buildList(jsonArray.length()) {
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                add(
                    School(
                        name = obj.getString("name"),
                        type = obj.getString("type"),
                        initial = obj.getString("initial"),
                        url = obj.optString("url", "")
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

private fun normalizeWebUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    return when {
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.isEmpty() -> "https://jwxt.cjlu.edu.cn/"
        else -> "https://$trimmed"
    }
}

@Composable
private fun SystemCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = description, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 16.sp)
                }
                Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun SchoolItem(
    school: School,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = school.name, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        val badgeBg = if (school.type == "zf") Color(0xFF3B82F6).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
        val badgeText = if (school.type == "zf") Color(0xFF3B82F6) else Color(0xFFD97706)
        val badgeLabel = if (school.type == "zf") "正方" else "青果"
        Box(
            modifier = Modifier
                .background(badgeBg, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = badgeLabel, color = badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(borderColor.copy(alpha = 0.3f)))
}

@Composable
fun WebViewImportScreen(
    isDark: Boolean,
    activeTimeNodes: List<TimeNode>,
    startDate: String,
    hasCourses: Boolean,
    predictiveBackEnabled: Boolean,
    initialStage: ImportStage = ImportStage.SELECT_MODE,
    onBack: () -> Unit,
    onImport: (String) -> Unit,
    onOverwriteImport: (String) -> Unit,
    onCreateScheduleAndImport: (String, String) -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val accent = LocalAccentColor.current ?: MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by remember { mutableStateOf(initialStage) }
    var selectedSchool by remember { mutableStateOf<School?>(null) }
    var selectedSystem by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var url by remember { mutableStateOf("https://jwxt.cjlu.edu.cn/") }
    var loadUrl by remember { mutableStateOf("https://jwxt.cjlu.edu.cn/") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var defaultMobileUserAgent by remember { mutableStateOf<String?>(null) }
    var desktopModeEnabled by remember { mutableStateOf(false) }

    var showSyncCreditsDialog by remember { mutableStateOf(false) }
    var showConfirmExamDialog by remember { mutableStateOf(false) }
    var pendingExtractedJson by remember { mutableStateOf<Pair<String, String>?>(null) } // json to schedulePrefix

    val schools = remember { loadSchoolsFromAssets(context) }
    val listState = rememberLazyListState()

    val xlsPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            parseQingGuoXls(inputStream)
                        }
                    }
                }
                result.fold(
                    onSuccess = { jsonData ->
                        if (!jsonData.isNullOrBlank() && jsonData != "[]") {
                            if (hasCourses) {
                                pendingExtractedJson = Pair(jsonData, "XLS导入课表")
                            } else {
                                onImport(jsonData)
                            }
                        } else {
                            Toast.makeText(context, "未提取到课程，请检查XLS文件格式", Toast.LENGTH_LONG).show()
                        }
                    },
                    onFailure = {
                        Toast.makeText(context, "读取或解析文件失败: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            parseCourseCsv(inputStream)
                        }
                    }
                }
                result.fold(
                    onSuccess = { jsonData ->
                        if (jsonData != null) {
                            pendingExtractedJson = Pair(jsonData, "CSV导入课表")
                        } else {
                            Toast.makeText(context, "未提取到课程，请检查 CSV 是否符合模板格式", Toast.LENGTH_LONG).show()
                        }
                    },
                    onFailure = {
                        Toast.makeText(context, "读取或解析 CSV 失败: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val navigateBack: () -> Unit = {
        when {
            stage == ImportStage.WEB_IMPORT && webViewRef?.canGoBack() == true -> {
                webViewRef?.goBack()
            }
            stage == ImportStage.CSV_IMPORT && initialStage == ImportStage.CSV_IMPORT -> {
                onBack()
            }
            stage != ImportStage.SELECT_MODE -> {
                stage = ImportStage.SELECT_MODE
            }
            selectedSystem != null -> {
                selectedSystem = null
            }
            else -> onBack()
        }
    }

    val backModifier = AppBackHandler(predictiveBackEnabled, navigateBack)

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
                webViewRef = null
            }
        }
    }

    val filteredSchools = remember(schools, searchQuery, activeTab, selectedSystem) {
        schools.filter { school ->
            val matchesSearch = school.name.contains(searchQuery, ignoreCase = true)
            val matchesTab = if (activeTab == 0) {
                when (selectedSystem) {
                    "zf" -> school.type == "zf"
                    "qg" -> school.type == "qg"
                    else -> false
                }
            } else true
            matchesSearch && matchesTab
        }.sortedWith(compareBy({ it.initial }, { it.name }))
    }

    val listItems = remember(filteredSchools) {
        buildList<Any> {
            filteredSchools.groupBy { it.initial }.forEach { (initial, schoolsForInitial) ->
                add(initial)
                addAll(schoolsForInitial)
            }
        }
    }

    val letterToFirstIndex = remember(listItems) {
        val map = mutableMapOf<Char, Int>()
        listItems.forEachIndexed { index, item ->
            if (item is String && item.length == 1) {
                val char = item[0].uppercaseChar()
                if (!map.containsKey(char)) map[char] = index
            }
        }
        map
    }
    val alphabet = remember { ('A'..'Z').toList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(backModifier)
            .background(if (isDark) BgDark else BgLight)
    ) {
        when (stage) {
            ImportStage.SELECT_MODE -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor,
                        modifier = Modifier
                            .clickable {
                                if (selectedSystem != null) selectedSystem = null else onBack()
                            }
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (selectedSystem) {
                            "zf" -> "正方教务学校"
                            "qg" -> "青果教务学校"
                            else -> "选择导入方式"
                        },
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedSystem == null) {
                    Text(
                        text = "常用学校",
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val commonSchoolsList = listOf(
                            School("中国计量大学", "zf", "Z", "https://jwxt.cjlu.edu.cn/"),
                            School("湖州师范大学", "zf", "H", "https://www.huznu.edu.cn/"),
                            School("兰州交通大学", "qg", "L", "")
                        )
                        commonSchoolsList.forEach { s ->
                            Button(
                                onClick = {
                                    selectedSchool = s
                                    if (s.type == "qg") {
                                        stage = ImportStage.XLS_IMPORT
                                    } else {
                                        val target = normalizeWebUrl(s.url)
                                        url = target
                                        loadUrl = target
                                        stage = ImportStage.WEB_IMPORT
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                                    contentColor = textColor
                                )
                            ) {
                                Text(
                                    text = s.name.replace("大学", ""),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("按教务分类", "按学校").forEachIndexed { index, title ->
                            val selected = activeTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) (if (isDark) Color(0xFF2C2C2E) else Color.White) else Color.Transparent)
                                    .clickable { activeTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (selected) textColor else textColor.copy(alpha = 0.6f),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                if (activeTab == 0 && selectedSystem == null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SystemCard(
                            title = "正方教务系统",
                            description = "最常见的教务系统，支持在线登录并直接提取课表与考试安排。",
                            icon = Icons.Rounded.Language,
                            gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                            onClick = { selectedSystem = "zf" }
                        )
                        SystemCard(
                            title = "青果教务系统",
                            description = "青果系统暂不支持网页端一键提取，统一通过在电脑端导出为 .xls 格式文件后导入。",
                            icon = Icons.Rounded.Description,
                            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                            onClick = {
                                selectedSchool = School("青果教务", "qg", "Q", "")
                                stage = ImportStage.XLS_IMPORT
                            }
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (activeTab == 0 && selectedSystem == "zf") {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("手动输入正方教务网址：", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = url,
                                            onValueChange = { url = it },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            singleLine = true,
                                            placeholder = { Text("https://...", fontSize = 13.sp) },
                                            textStyle = TextStyle(color = textColor, fontSize = 13.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
                                                focusedTextColor = textColor,
                                                unfocusedTextColor = textColor
                                            )
                                        )
                                        Button(
                                            onClick = {
                                                val targetUrl = normalizeWebUrl(url)
                                                url = targetUrl
                                                loadUrl = targetUrl
                                                selectedSchool = null
                                                stage = ImportStage.WEB_IMPORT
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = if (isDark) BgDark else BgLight),
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Text("进入", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("搜索学校名称...", color = textColor.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = textColor.copy(alpha = 0.5f)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = null, tint = textColor.copy(alpha = 0.5f))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = borderColor,
                                unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = textColor
                            )
                        )

                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
                                items(listItems) { item ->
                                    if (item is String && item.length == 1) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = item, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    } else if (item is School) {
                                        SchoolItem(
                                            school = item,
                                            isDark = isDark,
                                            onClick = {
                                                selectedSchool = item
                                                if (item.type == "qg") {
                                                    stage = ImportStage.XLS_IMPORT
                                                } else {
                                                    if (item.url.isNotEmpty()) {
                                                        val target = normalizeWebUrl(item.url)
                                                        url = target
                                                        loadUrl = target
                                                    } else {
                                                        val query = Uri.encode("${item.name}教务系统")
                                                        val searchUrl = "https://m.baidu.com/s?word=$query"
                                                        url = searchUrl
                                                        loadUrl = searchUrl
                                                        Toast.makeText(context, "已自动搜索该校教务系统链接，请在页面中点击进入", Toast.LENGTH_LONG).show()
                                                    }
                                                    stage = ImportStage.WEB_IMPORT
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            var alphabetHeight by remember { mutableIntStateOf(1) }
                            Column(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp)
                                    .onGloballyPositioned { alphabetHeight = it.size.height.coerceAtLeast(1) }
                                    .pointerInput(letterToFirstIndex) {
                                        detectDragGestures { change, _ ->
                                            val y = change.position.y
                                            val index = ((y / alphabetHeight) * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                                            val letter = alphabet[index]
                                            val targetIndex = alphabet.filter { it >= letter }.mapNotNull { letterToFirstIndex[it] }.firstOrNull()
                                            if (targetIndex != null) {
                                                coroutineScope.launch { listState.scrollToItem(targetIndex) }
                                            }
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                alphabet.forEach { letter ->
                                    val hasSchools = letterToFirstIndex.containsKey(letter)
                                    Text(
                                        text = letter.toString(),
                                        color = if (hasSchools) textColor else textColor.copy(alpha = 0.25f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable(enabled = hasSchools) {
                                                letterToFirstIndex[letter]?.let { index ->
                                                    coroutineScope.launch { listState.scrollToItem(index) }
                                                }
                                            }
                                            .padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ImportStage.WEB_IMPORT -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor,
                        modifier = Modifier.clickable { navigateBack() }.padding(8.dp)
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.weight(1f).height(50.dp),
                        singleLine = true,
                        textStyle = TextStyle(color = textColor),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = textColor
                        )
                    )
                    TextButton(onClick = {
                        val targetUrl = normalizeWebUrl(url)
                        url = targetUrl
                        loadUrl = targetUrl
                    }) {
                        Text("访问", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { val t = "https://jwxt.cjlu.edu.cn/"; url = t; loadUrl = t },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) { Text("计量大学", fontSize = 12.sp, color = textColor) }
                    OutlinedButton(
                        onClick = { val t = "https://www.huznu.edu.cn/"; url = t; loadUrl = t },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) { Text("湖州师范", fontSize = 12.sp, color = textColor) }
                    OutlinedButton(
                        onClick = { stage = ImportStage.XLS_IMPORT },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) { Text("兰州交大", fontSize = 12.sp, color = textColor) }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                defaultMobileUserAgent = settings.userAgentString
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.allowContentAccess = true
                                settings.allowFileAccess = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, pageUrl, favicon)
                                        if (!desktopModeEnabled) {
                                            view?.evaluateJavascript(ZhengfangJsScripts.UNIVERSAL_MOBILE_FIX_JS, null)
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                        super.onPageFinished(view, pageUrl)
                                        CookieManager.getInstance().flush()
                                        if (pageUrl != null) {
                                            url = pageUrl
                                        }
                                        if (SchoolCompatRegistry.shouldApplyLegacyDesktopWidth(pageUrl)) {
                                            view?.evaluateJavascript(ZhengfangJsScripts.LEGACY_DESKTOP_WIDTH_JS, null)
                                        }
                                        if (!desktopModeEnabled) {
                                            view?.evaluateJavascript(ZhengfangJsScripts.UNIVERSAL_MOBILE_FIX_JS, null)
                                        }
                                    }

                                    override fun doUpdateVisitedHistory(view: WebView?, pageUrl: String?, isReload: Boolean) {
                                        super.doUpdateVisitedHistory(view, pageUrl, isReload)
                                        if (!desktopModeEnabled) {
                                            view?.evaluateJavascript(ZhengfangJsScripts.UNIVERSAL_MOBILE_FIX_JS, null)
                                        }
                                    }

                                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                        return SchoolCompatRegistry.interceptResourceIfNeeded(request?.url)
                                            ?: super.shouldInterceptRequest(view, request)
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            (ctx as? ComponentActivity)?.runOnUiThread {
                                                Toast.makeText(ctx, "教务系统加载失败：${error?.description ?: "未知错误"}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(message: ConsoleMessage): Boolean = true
                                }
                                addJavascriptInterface(object : Any() {
                                    @JavascriptInterface
                                    fun passData(data: String) {
                                        (ctx as? ComponentActivity)?.runOnUiThread {
                                            if (data.startsWith("ERROR:")) {
                                                Toast.makeText(ctx, data.removePrefix("ERROR:"), Toast.LENGTH_LONG).show()
                                            } else if (hasCourses) {
                                                pendingExtractedJson = Pair(data, "教务导入课表")
                                            } else {
                                                onImport(data)
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun passExamData(html: String) {
                                        (ctx as? ComponentActivity)?.runOnUiThread {
                                            val (jsonData, errorMsg) = parseZhengfangExamHtml(
                                                html.byteInputStream(),
                                                activeTimeNodes,
                                                startDate
                                            )
                                            if (errorMsg != null) {
                                                Toast.makeText(ctx, errorMsg, Toast.LENGTH_LONG).show()
                                            } else if (jsonData != null && jsonData != "[]") {
                                                onImport(jsonData)
                                            } else {
                                                Toast.makeText(ctx, "未提取到任何有效的考试安排", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }, "Android")

                                webViewRef = this
                                tag = loadUrl
                                loadUrl(loadUrl)
                            }
                        },
                        update = { webView ->
                            if (webView.tag != loadUrl) {
                                webView.tag = loadUrl
                                webView.loadUrl(loadUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showSyncCreditsDialog = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = if (isDark) BgDark else BgLight)
                    ) {
                        Text("提取课表", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (hasCourses) showConfirmExamDialog = true
                            else ZhengfangJsScripts.executeZhengfangExamJs(webViewRef)
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF1F2937) else Color(0xFFF3F4F6),
                            contentColor = textColor
                        ),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Text("提取考试", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val webView = webViewRef
                            if (webView == null) {
                                Toast.makeText(context, "网页尚未加载完成", Toast.LENGTH_SHORT).show()
                            } else {
                                val nextDesktop = !desktopModeEnabled
                                desktopModeEnabled = nextDesktop
                                if (nextDesktop) {
                                    webView.settings.userAgentString = DESKTOP_USER_AGENT
                                    webView.settings.useWideViewPort = true
                                    webView.settings.loadWithOverviewMode = false
                                    webView.settings.textZoom = 100
                                    Toast.makeText(context, "已切换到桌面模式（将刷新页面）", Toast.LENGTH_SHORT).show()
                                } else {
                                    webView.settings.userAgentString = defaultMobileUserAgent
                                    webView.settings.useWideViewPort = true
                                    webView.settings.loadWithOverviewMode = true
                                    Toast.makeText(context, "已恢复移动适配模式（将刷新页面）", Toast.LENGTH_SHORT).show()
                                }
                                webView.reload()
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(if (desktopModeEnabled) 1.5.dp else 1.dp, if (desktopModeEnabled) accent else borderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (desktopModeEnabled) accent else textColor)
                    ) {
                        Text(if (desktopModeEnabled) "移动模式" else "桌面模式", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            ImportStage.XLS_IMPORT -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor,
                        modifier = Modifier.clickable { navigateBack() }.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (selectedSchool != null) "${selectedSchool!!.name}导入" else "本地 XLS 导入",
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val promptText = if (selectedSchool != null) {
                        "由于「${selectedSchool!!.name}」使用的是青果教务系统，受环境限制，请先在电脑端或浏览器中导出课表为 .xls 文件，发送到手机后点击下方按钮导入。"
                    } else {
                        "由于该校教务系统环境限制，请在电脑端或浏览器自行导出课表为 .xls 文件，发送到手机后通过下方按钮导入。"
                    }
                    Text(
                        text = promptText,
                        color = textColor,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 24.dp),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Button(
                        onClick = { xlsPickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = if (isDark) BgDark else BgLight)
                    ) {
                        Text("选择 XLS 文件并导入", fontWeight = FontWeight.Bold)
                    }
                }
            }

            ImportStage.CSV_IMPORT -> {
                val uriHandler = LocalUriHandler.current
                var hasReadCsvInstructions by remember { mutableStateOf(false) }
                var isDownloadingTemplate by remember { mutableStateOf(false) }
                var showDownloadSuccessDialog by remember { mutableStateOf(false) }
                var downloadedTemplateFile by remember { mutableStateOf<File?>(null) }
                val csvAccent = LocalAccentColor.current ?: if (isDark) Color(0xFFF472B6) else Color(0xFF9A5264)

                val startDownload: () -> Unit = {
                    if (!isDownloadingTemplate) {
                        isDownloadingTemplate = true
                        coroutineScope.launch {
                            val file = downloadCsvTemplate(context, CSV_TEMPLATE_URL)
                            isDownloadingTemplate = false
                            if (file != null && file.exists()) {
                                downloadedTemplateFile = file
                                showDownloadSuccessDialog = true
                            } else {
                                Toast.makeText(context, "直接下载未成功，已为您转至浏览器下载", Toast.LENGTH_SHORT).show()
                                uriHandler.openUri(CSV_TEMPLATE_URL)
                            }
                        }
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { startDownload() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor,
                        modifier = Modifier.clickable { navigateBack() }.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("从 CSV 文件导入", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 32.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "请直接在模板的基础上修改，不要自己新建。应填的内容一定不能为空，如果没有该字段的内容，则填“无”。",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        lineHeight = 25.sp
                    )
                    Text(
                        "在电脑上把 Excel 转 CSV 只需「另存为 -> 选择 CSV 格式」即可。Excel 自动把周数（如 1-16）转成日期（如 1月16日 / 1/16）的情况已支持自动还原。",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        lineHeight = 25.sp
                    )
                    Text(
                        "有任何问题请先点击下方的获取模板按钮。",
                        color = textColor.copy(alpha = 0.75f),
                        fontSize = 15.sp,
                        lineHeight = 25.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { hasReadCsvInstructions = !hasReadCsvInstructions },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = hasReadCsvInstructions,
                            onCheckedChange = { hasReadCsvInstructions = it },
                            colors = CheckboxDefaults.colors(checkedColor = csvAccent, checkmarkColor = Color.White)
                        )
                        Text("我已经仔细阅读过上面的说明", color = textColor, fontSize = 15.sp)
                    }
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                startDownload()
                            }
                        },
                        enabled = !isDownloadingTemplate,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = csvAccent, contentColor = Color.White)
                    ) {
                        if (isDownloadingTemplate) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在下载模板...", fontSize = 16.sp)
                        } else {
                            Text("点击此处获取模板", fontSize = 16.sp)
                        }
                    }
                    Button(
                        onClick = { csvPickerLauncher.launch("*/*") },
                        enabled = hasReadCsvInstructions,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = csvAccent,
                            contentColor = Color.White,
                            disabledContainerColor = csvAccent.copy(alpha = 0.35f),
                            disabledContentColor = Color.White.copy(alpha = 0.75f)
                        )
                    ) {
                        Text("选择 CSV 文件", fontSize = 16.sp)
                    }
                }

                if (showDownloadSuccessDialog) {
                    AlertDialog(
                        onDismissRequest = { showDownloadSuccessDialog = false },
                        icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp)) },
                        title = { Text("课表模板下载完成", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center) },
                        text = {
                            Column {
                                Text("课表模板已成功保存至手机系统「下载」目录：", fontSize = 14.sp, color = textColor.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Description, contentDescription = null, tint = csvAccent, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("课表模板.csv", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                                            Text("存储路径: Download/课表模板.csv", fontSize = 12.sp, color = textColor.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("是否立即前往系统文件管理器查看此文件？\n您可以对其进行编辑、重命名或分享发送给同学。", fontSize = 13.sp, lineHeight = 18.sp, color = textColor.copy(alpha = 0.7f))
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDownloadSuccessDialog = false
                                    openFileManagerForFile(context, downloadedTemplateFile)
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = csvAccent, contentColor = Color.White)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("前往文件管理器", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDownloadSuccessDialog = false }) { Text("稍后再说", color = textColor.copy(alpha = 0.6f)) }
                        },
                        containerColor = if (isDark) Color(0xFF18181B) else Color.White,
                        titleContentColor = textColor,
                        textContentColor = textColor.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            }
        }

        if (pendingExtractedJson != null) {
            val (json, defaultNewName) = pendingExtractedJson!!
            AlertDialog(
                onDismissRequest = { pendingExtractedJson = null },
                title = { Text("请选择导入课表的方式", fontWeight = FontWeight.Bold) },
                text = { Text("当前课表已有课程，可选择直接覆盖当前课表，或新建一个独立课表保存。") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingExtractedJson = null
                        onOverwriteImport(json)
                    }) {
                        Text("覆盖当前课表", color = textColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            pendingExtractedJson = null
                            onCreateScheduleAndImport(json, defaultNewName)
                        }) {
                            Text("新建课表再导入", color = textColor)
                        }
                        TextButton(onClick = { pendingExtractedJson = null }) {
                            Text("取消", color = Color(0xFFDC2626))
                        }
                    }
                },
                containerColor = if (isDark) Color(0xFF18181B) else Color.White,
                titleContentColor = textColor,
                textContentColor = textColor.copy(alpha = 0.8f)
            )
        }

        if (showSyncCreditsDialog) {
            AlertDialog(
                onDismissRequest = { showSyncCreditsDialog = false },
                title = { Text("学分导入设置", fontWeight = FontWeight.Bold) },
                text = { Text("是否同步导入课程学分？\n导入后学分将显示在课程名称后。") },
                confirmButton = {
                    TextButton(onClick = {
                        showSyncCreditsDialog = false
                        ZhengfangJsScripts.executeZhengfangImportJs(loadUrl, webViewRef, syncCredits = true)
                    }) {
                        Text("同步导入", color = textColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSyncCreditsDialog = false
                        ZhengfangJsScripts.executeZhengfangImportJs(loadUrl, webViewRef, syncCredits = false)
                    }) {
                        Text("忽略学分", color = textColor.copy(alpha = 0.6f))
                    }
                },
                containerColor = if (isDark) Color(0xFF18181B) else Color.White,
                titleContentColor = textColor,
                textContentColor = textColor.copy(alpha = 0.8f)
            )
        }

        if (showConfirmExamDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmExamDialog = false },
                title = { Text("确认导入考试", fontWeight = FontWeight.Bold) },
                text = { Text("检测到您当前课表中已经有课程。导入考试安排将作为日程叠加在现有课表上，是否确认继续导入？") },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmExamDialog = false
                        ZhengfangJsScripts.executeZhengfangExamJs(webViewRef)
                    }) {
                        Text("确认导入", color = textColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmExamDialog = false }) {
                        Text("取消", color = textColor.copy(alpha = 0.6f))
                    }
                },
                containerColor = if (isDark) Color(0xFF18181B) else Color.White,
                titleContentColor = textColor,
                textContentColor = textColor.copy(alpha = 0.8f)
            )
        }
    }
}
