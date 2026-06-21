package com.novel.reader.ui.reader

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novel.reader.data.Chapter
import com.novel.reader.ui.theme.DarkBg
import com.novel.reader.ui.theme.DarkText
import com.novel.reader.ui.theme.PaperBg
import com.novel.reader.ui.theme.PaperText
import com.novel.reader.ui.theme.SepiaBg
import com.novel.reader.ui.theme.SepiaText
import kotlinx.coroutines.launch

// 主题方案：0=浅色(纸) 1=深色 2=护眼米黄
private data class ReaderPalette(
    val bg: Color,
    val text: Color,
    val secondary: Color,
    val isDark: Boolean
)

@Composable
private fun resolvePalette(themeCode: Int): ReaderPalette = when (themeCode) {
    1 -> ReaderPalette(DarkBg, DarkText, DarkText.copy(alpha = 0.6f), isDark = true)
    2 -> ReaderPalette(SepiaBg, SepiaText, SepiaText.copy(alpha = 0.6f), isDark = false)
    else -> ReaderPalette(PaperBg, PaperText, PaperText.copy(alpha = 0.5f), isDark = false)
}

@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val prefs = viewModel.prefs

    var readingMode by remember { mutableIntStateOf(prefs.readingMode) }
    var themeCode by remember { mutableIntStateOf(prefs.darkTheme) }
    var fontSize by remember { mutableIntStateOf(prefs.fontSize) }
    var lineHeight by remember { mutableFloatStateOf(prefs.lineHeight) }
    var menuVisible by remember { mutableStateOf(false) }

    // 系统状态栏 / 沉浸式控制
    val view = LocalView.current
    val controller = remember(view) {
        runCatching {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }.getOrNull()
    }
    DisposableEffect(menuVisible) {
        val c = controller
        if (c != null) {
            if (menuVisible) c.show(WindowInsetsCompat.Type.systemBars())
            else c.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // 持久化偏好
    LaunchedEffect(readingMode) { prefs.readingMode = readingMode }
    LaunchedEffect(themeCode) { prefs.darkTheme = themeCode }
    LaunchedEffect(fontSize) { prefs.fontSize = fontSize }
    LaunchedEffect(lineHeight) { prefs.lineHeight = lineHeight }

    // 首次加载
    LaunchedEffect(bookId) { viewModel.load(bookId) }

    val resolvedThemeCode = if (themeCode == -1) (if (isSystemInDarkTheme()) 1 else 0) else themeCode
    val palette = resolvePalette(resolvedThemeCode)

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.bg)
            // 适配挖孔屏：内容避开刘海 / 挖孔区域；菜单隐藏时全屏沉浸阅读
            .windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        when (val s = state) {
            is ReaderUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = palette.text)
            }
            is ReaderUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(s.message, color = palette.text)
            }
            is ReaderUiState.Ready -> {
                val chapter = s.chapters[s.chapterIndex]
                ReaderContent(
                    chapter = chapter,
                    chapterIndex = s.chapterIndex,
                    totalChapters = s.chapters.size,
                    readingMode = readingMode,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    palette = palette,
                    onToggleMenu = { menuVisible = !menuVisible },
                    onPrevChapter = { viewModel.prevChapter() },
                    onNextChapter = { viewModel.nextChapter() },
                    onChapterChange = { viewModel.setChapter(it) }
                )
            }
        }

        // 顶部/底部菜单（仅 menuVisible 时显示）
        val ready = state as? ReaderUiState.Ready
        if (menuVisible && ready != null) {
            ReaderMenus(
                title = ready.book.title,
                chapterTitle = ready.chapters[ready.chapterIndex].title,
                chapterIndex = ready.chapterIndex,
                totalChapters = ready.chapters.size,
                readingMode = readingMode,
                themeCode = resolvedThemeCode,
                fontSize = fontSize,
                onBack = onBack,
                onModeChange = { readingMode = it },
                onThemeChange = { themeCode = it },
                onFontInc = { fontSize = (fontSize + 1).coerceAtMost(36) },
                onFontDec = { fontSize = (fontSize - 1).coerceAtLeast(12) },
                onPrevChapter = { viewModel.prevChapter() },
                onNextChapter = { viewModel.nextChapter() },
                onChapterChange = { viewModel.setChapter(it) },
                palette = palette
            )
        }
    }
}

// ---------- 内容区 ----------
@Composable
private fun ReaderContent(
    chapter: Chapter,
    chapterIndex: Int,
    totalChapters: Int,
    readingMode: Int,
    fontSize: Int,
    lineHeight: Float,
    palette: ReaderPalette,
    onToggleMenu: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onChapterChange: (Int) -> Unit
) {
    when (readingMode) {
        0 -> PageReader(
            chapter = chapter,
            orientation = PageOrientation.Horizontal,
            fontSize = fontSize,
            lineHeight = lineHeight,
            palette = palette,
            onToggleMenu = onToggleMenu,
            onPrevChapter = onPrevChapter,
            onNextChapter = onNextChapter
        )
        1 -> PageReader(
            chapter = chapter,
            orientation = PageOrientation.Vertical,
            fontSize = fontSize,
            lineHeight = lineHeight,
            palette = palette,
            onToggleMenu = onToggleMenu,
            onPrevChapter = onPrevChapter,
            onNextChapter = onNextChapter
        )
        else -> ScrollReader(
            chapter = chapter,
            chapterIndex = chapterIndex,
            totalChapters = totalChapters,
            fontSize = fontSize,
            lineHeight = lineHeight,
            palette = palette,
            onToggleMenu = onToggleMenu,
            onPrevChapter = onPrevChapter,
            onNextChapter = onNextChapter
        )
    }
}

private enum class PageOrientation { Horizontal, Vertical }

@Composable
private fun PageReader(
    chapter: Chapter,
    orientation: PageOrientation,
    fontSize: Int,
    lineHeight: Float,
    palette: ReaderPalette,
    onToggleMenu: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit
) {
    val config = LocalConfiguration.current
    // 估算每页字符容量（CJK 友好）
    val charsPerPage = remember(chapter, fontSize, lineHeight, config) {
        val availW = config.screenWidthDp - 48          // 左右各 24dp 内边距
        val availH = config.screenHeightDp - 96         // 上下预留
        val charsPerLine = (availW / (fontSize * 0.95)).toInt().coerceAtLeast(8)
        val linesPerPage = (availH / (fontSize * lineHeight * 1.05)).toInt().coerceAtLeast(4)
        (charsPerLine * linesPerPage * 0.9).toInt().coerceAtLeast(80)
    }

    val pages = remember(chapter, charsPerPage) {
        paginate(toParagraphs(chapter.content), charsPerPage)
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    fun goNext() {
        if (pagerState.currentPage < pages.lastIndex) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        } else {
            onNextChapter(); scope.launch { pagerState.scrollToPage(0) }
        }
    }

    fun goPrev() {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            onPrevChapter(); scope.launch { pagerState.scrollToPage(0) }
        }
    }

    val pageContent: @Composable (Int) -> Unit = { index ->
        PageView(
            paragraphs = pages[index],
            pageIndex = index,
            pageCount = pages.size,
            fontSize = fontSize,
            lineHeight = lineHeight,
            palette = palette,
            onTapLeft = ::goPrev,
            onTapCenter = onToggleMenu,
            onTapRight = ::goNext,
            modifier = Modifier.fillMaxSize()
        )
    }

    if (orientation == PageOrientation.Horizontal) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageContent(it) }
    } else {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageContent(it) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageView(
    paragraphs: List<String>,
    pageIndex: Int,
    pageCount: Int,
    fontSize: Int,
    lineHeight: Float,
    palette: ReaderPalette,
    onTapLeft: () -> Unit,
    onTapCenter: () -> Unit,
    onTapRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noRipple = remember { MutableInteractionSource() }
    val noRipple2 = remember { MutableInteractionSource() }
    val noRipple3 = remember { MutableInteractionSource() }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Text(
                text = buildContent(paragraphs, fontSize, lineHeight, palette.text),
                style = TextStyle(
                    color = palette.text,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * lineHeight).sp
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${pageIndex + 1} / $pageCount",
                fontSize = 11.sp,
                color = palette.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        // 三分点击区：左/中/右
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(noRipple, indication = null) { onTapLeft() }
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(noRipple2, indication = null) { onTapCenter() }
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(noRipple3, indication = null) { onTapRight() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScrollReader(
    chapter: Chapter,
    chapterIndex: Int,
    totalChapters: Int,
    fontSize: Int,
    lineHeight: Float,
    palette: ReaderPalette,
    onToggleMenu: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit
) {
    val paragraphs = remember(chapter) { toParagraphs(chapter.content) }
    val noRipple = remember { MutableInteractionSource() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clickable(noRipple, indication = null) { onToggleMenu() },
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy((fontSize * lineHeight * 0.6f).dp)
        ) {
            item {
                Text(
                    chapter.title,
                    fontSize = (fontSize + 4).sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.text,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            items(paragraphs) { p ->
                Text(
                    text = p,
                    color = palette.text,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * lineHeight).sp
                )
            }
            // 章末导航
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AssistChip(
                        onClick = onPrevChapter,
                        label = { Text("上一章") },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                null,
                                Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = palette.text.copy(alpha = 0.1f)
                        )
                    )
                    Spacer(Modifier.weight(1f))
                    AssistChip(
                        onClick = onNextChapter,
                        label = { Text("下一章") },
                        trailingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                null,
                                Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = palette.text.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
}

// ---------- 菜单 ----------
@Composable
private fun ReaderMenus(
    title: String,
    chapterTitle: String,
    chapterIndex: Int,
    totalChapters: Int,
    readingMode: Int,
    themeCode: Int,
    fontSize: Int,
    onBack: () -> Unit,
    onModeChange: (Int) -> Unit,
    onThemeChange: (Int) -> Unit,
    onFontInc: () -> Unit,
    onFontDec: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onChapterChange: (Int) -> Unit,
    palette: ReaderPalette
) {
    val barBg = if (palette.isDark) Color(0xE6000000) else Color(0xF2FFFFFF)
    val barText = if (palette.isDark) Color.White else Color.Black

    Column(Modifier.fillMaxSize()) {
        // 顶部栏
        Surface(color = barBg, shadowElevation = 4.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = barText)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, color = barText, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        chapterTitle,
                        color = barText.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onPrevChapter) {
                    Icon(Icons.Filled.SkipPrevious, "上一章", tint = barText)
                }
                IconButton(onClick = onNextChapter) {
                    Icon(Icons.Filled.SkipNext, "下一章", tint = barText)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        // 底部控制栏
        Surface(color = barBg, shadowElevation = 8.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 章节进度
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("第 ${chapterIndex + 1} 章", color = barText, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "共 $totalChapters 章",
                        color = barText.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Slider(
                    value = chapterIndex.toFloat(),
                    onValueChange = { onChapterChange(it.toInt()) },
                    valueRange = 0f..(totalChapters - 1).toFloat().coerceAtLeast(0f),
                    steps = (totalChapters - 2).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // 阅读模式
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ModeButton("左右翻页", readingMode == 0, barText) { onModeChange(0) }
                    ModeButton("上下翻页", readingMode == 1, barText) { onModeChange(1) }
                    ModeButton("连续滚动", readingMode == 2, barText) { onModeChange(2) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // 字号
                    IconButton(onClick = onFontDec) {
                        Icon(Icons.Filled.Remove, "减小", tint = barText)
                    }
                    Text(
                        "字号 $fontSize",
                        color = barText,
                        fontSize = 13.sp,
                        modifier = Modifier.width(76.dp)
                    )
                    IconButton(onClick = onFontInc) {
                        Icon(Icons.Filled.Add, "增大", tint = barText)
                    }
                    Spacer(Modifier.weight(1f))
                    // 主题切换
                    ThemeDot(Color(0xFFF7F4ED), themeCode == 0) { onThemeChange(0) }
                    Spacer(Modifier.width(10.dp))
                    ThemeDot(Color(0xFF15151A), themeCode == 1) { onThemeChange(1) }
                    Spacer(Modifier.width(10.dp))
                    ThemeDot(Color(0xFFEFE4D2), themeCode == 2) { onThemeChange(2) }
                }
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val bg = if (selected) color.copy(alpha = 0.18f) else Color.Transparent
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = color,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ThemeDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    val checkColor = if (color.red + color.green + color.blue < 1.5f) Color.White else Color.Black
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, null, tint = checkColor, modifier = Modifier.size(16.dp))
        }
    }
}

// ---------- 工具 ----------
@Suppress("unused")
private fun buildContent(
    paragraphs: List<String>,
    fontSize: Int,
    @Suppress("UNUSED_PARAMETER") lineHeight: Float,
    color: Color
): AnnotatedString = buildAnnotatedString {
    val style = SpanStyle(
        color = color,
        fontSize = fontSize.sp
    )
    paragraphs.forEachIndexed { i, p ->
        if (i > 0) append("\n\n")
        withStyle(style) {
            append(p)
        }
    }
}
