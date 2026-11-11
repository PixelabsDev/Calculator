package com.example

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calculator.CalculatorViewModel
import com.example.data.HistoryEntry
import com.example.report.ReportIssueDialog
import com.example.ui.theme.MyApplicationTheme

val LocalButtonAspectRatio = compositionLocalOf { 1f }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: CalculatorViewModel = viewModel()
            val isScreenshotBlocked by viewModel.isScreenshotBlocked.collectAsState()

            LaunchedEffect(isScreenshotBlocked) {
                if (isScreenshotBlocked) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                com.example.calculator.ThemeMode.SYSTEM -> systemDark
                com.example.calculator.ThemeMode.LIGHT -> false
                com.example.calculator.ThemeMode.DARK -> true
            }
            MyApplicationTheme(darkTheme = isDark) {
                CalculatorAppScreen(viewModel)
            }
        }
    }
}

@Composable
fun CalculatorAppScreen(viewModel: CalculatorViewModel = viewModel()) {
    val context = LocalContext.current
    val input by viewModel.inputState.collectAsState()
    val result by viewModel.resultState.collectAsState()
    val isRadMode by viewModel.isRadMode.collectAsState()
    val isInvMode by viewModel.isInvMode.collectAsState()
    val isScientificExpanded by viewModel.isScientificExpanded.collectAsState()
    val historyList by viewModel.historyState.collectAsState()
    val isScreenshotBlocked by viewModel.isScreenshotBlocked.collectAsState()

    var showHistoryDrawer by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // Theme responsive coloring setup
    val themeMode by viewModel.themeMode.collectAsState()
    val isDark = when (themeMode) {
        com.example.calculator.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        com.example.calculator.ThemeMode.LIGHT -> false
        com.example.calculator.ThemeMode.DARK -> true
    }

    // High-contrast refined palette (Light vs Dark adaptation)
    val displayBgColor = if (isDark) Color(0xFF0F0E14) else Color(0xFFFFFFFF)
    val appBgColor = if (isDark) Color(0xFF0F0E14) else Color(0xFFFFFFFF)
    val keypadBgColor = if (isDark) Color(0xFF14131A) else Color(0xFFFAF8FC)

    // Number keys coloring (0-9, .)
    val numberBtnBg = if (isDark) Color(0xFF2C2738) else Color(0xFFFFFFFF)
    val numberBtnTxt = if (isDark) Color(0xFFFFFFFF) else Color(0xFF191622)

    // Basic operator coloring (+, -, ×, ÷, ( ), %)
    val operatorBtnBg = if (isDark) Color(0xFF493E5C) else Color(0xFFDCD2EC)
    val operatorBtnTxt = if (isDark) Color(0xFFF3EDFF) else Color(0xFF25183E)

    // Scientific specific coloring (√, π, ^, !, sin, cos, tan, ln, log, e)
    val scientificBtnBg = if (isDark) Color(0xFF383048) else Color(0xFFDCD2EC)
    val scientificBtnTxt = if (isDark) Color(0xFFD6C4FF) else Color(0xFF321F53)

    // Equals high-contrast accent button highlighting
    val equalsBtnBg = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    val equalsBtnTxt = if (isDark) Color(0xFF381E72) else Color(0xFFFFFFFF)

    val iconColor = if (isDark) Color(0xFFF3EDFF) else Color(0xFF191622)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor)
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val vibrate = rememberVibrator()

                // 1. Top Bar Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = if (isPortrait) 4.dp else 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            vibrate(false)
                            showHistoryDrawer = !showHistoryDrawer
                        },
                        modifier = Modifier
                            .semantics { contentDescription = "Calculation History" }
                            .testTag("history_button")
                    ) {
                        HistoryIcon(color = iconColor)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleThemeMode() },
                            modifier = Modifier
                                .semantics { contentDescription = "Toggle Theme" }
                                .testTag("theme_toggle_button")
                        ) {
                            when (themeMode) {
                                com.example.calculator.ThemeMode.SYSTEM -> SystemModeIcon(color = iconColor)
                                com.example.calculator.ThemeMode.LIGHT -> LightModeIcon(color = iconColor)
                                com.example.calculator.ThemeMode.DARK -> DarkModeIcon(color = iconColor)
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .semantics { contentDescription = "More Options" }
                                    .testTag("more_options_button")
                            ) {
                                ThreeDotMenuIcon(color = iconColor)
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Privacy Policy & Terms")
                                            Text("↗", color = iconColor.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, "https://policies.pixelabs.in/calculator/".toUri()).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                        showMenu = false
                                    },
                                    modifier = Modifier.testTag("menu_privacy_terms")
                                )
                                DropMenuItemScreenshotBlock(
                                    isScreenshotBlocked = isScreenshotBlocked,
                                    onToggle = {
                                        viewModel.toggleScreenshotBlock()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Send Feedback") },
                                    onClick = {
                                        showReportDialog = true
                                        showMenu = false
                                    },
                                    modifier = Modifier.testTag("menu_report_issue")
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear History") },
                                    onClick = {
                                        viewModel.clearHistory()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isRadMode) "Switch to Degrees" else "Switch to Radians") },
                                    onClick = {
                                        viewModel.toggleRadMode()
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = showHistoryDrawer,
                    transitionSpec = {
                        if (targetState) {
                            // Opening History: spring physics slide down from top + gentle fade in
                            (slideInVertically(
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                                initialOffsetY = { -it }
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                            )).togetherWith(
                                slideOutVertically(
                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                                    targetOffsetY = { it / 3 }
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                )
                            )
                        } else {
                            // Closing History: spring physics slide up to top + smooth fade in for calculator
                            (slideInVertically(
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                                initialOffsetY = { it / 3 }
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                            )).togetherWith(
                                slideOutVertically(
                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                                    targetOffsetY = { -it }
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "historyPageTransition"
                ) { isHistoryOpen ->
                    if (isHistoryOpen) {
                        // Full Screen History View with Two-Way Swipe Dismissal
                        val historyNestedScroll = remember {
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    if (available.y < -35f) {
                                        showHistoryDrawer = false
                                    }
                                    return Offset.Zero
                                }
                            }
                        }
                        var historyDragAccumulator by remember { mutableFloatStateOf(0f) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(appBgColor)
                                .nestedScroll(historyNestedScroll)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = { historyDragAccumulator = 0f },
                                        onDragEnd = {
                                            if (historyDragAccumulator < -40f) {
                                                showHistoryDrawer = false
                                            }
                                            historyDragAccumulator = 0f
                                        },
                                        onDragCancel = { historyDragAccumulator = 0f },
                                        onVerticalDrag = { change, dragAmount ->
                                            historyDragAccumulator += dragAmount
                                            if (historyDragAccumulator < -80f) {
                                                showHistoryDrawer = false
                                                change.consume()
                                            }
                                        }
                                    )
                                }
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            if (historyList.isNotEmpty()) {
                                Text(
                                    text = "Calculation History",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFB0AAB8) else Color(0xFF5A5565),
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    items(historyList, key = { it.id }) { item ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setInput(item.expression)
                                                    showHistoryDrawer = false
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = item.expression,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                textAlign = TextAlign.End,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.result,
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = if (item.result.length > 12) 24.sp else 32.sp
                                                ),
                                                textAlign = TextAlign.End,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Your history is clean.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
                                        )
                                    )
                                }
                            }
                        }

                        // Pinned Bottom Sheet for "Current expression" with responsive drag handle physics & single-line auto-scaling
                        var handleDragOffset by remember { mutableFloatStateOf(0f) }
                        val animatedHandleOffset by animateFloatAsState(
                            targetValue = handleDragOffset,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                            label = "sheetHandleDragOffset"
                        )

                        val currentPreviewText = if (result.isNotEmpty()) result else if (input.isNotEmpty()) input else "0"
                        val bottomPreviewScroll = rememberScrollState()
                        LaunchedEffect(currentPreviewText) {
                            if (currentPreviewText.isNotEmpty()) {
                                bottomPreviewScroll.animateScrollTo(bottomPreviewScroll.maxValue)
                            }
                        }

                        // Auto-scaling font size for large exponents / long expressions to prevent overflow
                        val bottomPreviewFontSize = when {
                            currentPreviewText.length <= 7 -> 30.sp
                            currentPreviewText.length <= 11 -> 24.sp
                            currentPreviewText.length <= 15 -> 19.sp
                            currentPreviewText.length <= 20 -> 16.sp
                            else -> 13.sp
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(0, animatedHandleOffset.roundToInt()) }
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = { handleDragOffset = 0f },
                                        onDragEnd = {
                                            if (handleDragOffset < -35f || handleDragOffset > 45f) {
                                                showHistoryDrawer = false
                                            }
                                            handleDragOffset = 0f
                                        },
                                        onDragCancel = { handleDragOffset = 0f },
                                        onVerticalDrag = { change, dragAmount ->
                                            handleDragOffset += dragAmount
                                            if (handleDragOffset < -70f || handleDragOffset > 90f) {
                                                showHistoryDrawer = false
                                            }
                                        }
                                    )
                                }
                                .clickable { showHistoryDrawer = false },
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            color = if (isDark) Color(0xFF221E2C) else Color(0xFFEDE8F5)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp)
                            ) {
                                // Drag handle pill indicator
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .width(36.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background((if (isDark) Color(0xFF9E9B9F) else Color(0xFF7D7A7F)).copy(alpha = 0.6f))
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Current expression",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(bottomPreviewScroll),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = currentPreviewText,
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontSize = bottomPreviewFontSize,
                                                color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A),
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.End,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (isPortrait) {
                        // Standard Portrait Calculator View with swipe-down-to-history gesture
                        var calcSwipeDownAccumulator by remember { mutableFloatStateOf(0f) }
                        var collapsedKeypadHeightPx by remember(configuration.screenWidthDp) { mutableStateOf(0) }
                        val density = LocalDensity.current
                        val keypadTransition = updateTransition(
                            targetState = isScientificExpanded,
                            label = "scientificKeypadTransition"
                        )
                        val chevronRotation by keypadTransition.animateFloat(
                            transitionSpec = {
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            },
                            label = "chevronRotation"
                        ) { expanded -> if (expanded) 180f else 0f }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = { calcSwipeDownAccumulator = 0f },
                                        onDragEnd = {
                                            if (calcSwipeDownAccumulator > 50f) {
                                                showHistoryDrawer = true
                                            }
                                            calcSwipeDownAccumulator = 0f
                                        },
                                        onDragCancel = { calcSwipeDownAccumulator = 0f },
                                        onVerticalDrag = { change, dragAmount ->
                                            if (dragAmount > 0) {
                                                calcSwipeDownAccumulator += dragAmount
                                                if (calcSwipeDownAccumulator > 120f) {
                                                    showHistoryDrawer = true
                                                    change.consume()
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                        // Display Canvas Area with guaranteed minimum height to prevent any preview clipping
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .defaultMinSize(minHeight = 150.dp)
                                .background(displayBgColor)
                                .clickable { showHistoryDrawer = true }
                                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.End
                        ) {
                            // Active input expression with auto-scroll to trailing edge
                            val inputScrollState = rememberScrollState()
                            LaunchedEffect(input) {
                                if (input.isNotEmpty()) {
                                    inputScrollState.animateScrollTo(inputScrollState.maxValue)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(inputScrollState),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Text(
                                    text = if (input.isEmpty()) "0" else input,
                                    style = TextStyleForInput(input),
                                    color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier
                                        .testTag("calculation_input"),
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Live preview evaluation result with fluid slide & fade transition and horizontal scroll
                            val previewScrollState = rememberScrollState()
                            LaunchedEffect(result) {
                                if (result.isNotEmpty()) {
                                    previewScrollState.animateScrollTo(previewScrollState.maxValue)
                                }
                            }

                            AnimatedContent(
                                targetState = result,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        slideInVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) { it / 3 })
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing))
                                        )
                                },
                                label = "calculationPreviewAnim"
                            ) { previewText ->
                                if (previewText.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(previewScrollState),
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Text(
                                            text = previewText,
                                            style = TextStyleForOutput(previewText),
                                            color = if (isDark) Color(0xFFD6C4FF) else Color(0xFF6750A4),
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier
                                                .testTag("calculation_preview"),
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                }
                            }
                        }

                        // Minimal Chevron Expand Button & Mode Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    vibrate(false)
                                    viewModel.toggleScientific()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .semantics { contentDescription = if (isScientificExpanded) "Collapse scientific keypad" else "Expand scientific keypad" }
                                    .testTag("scientific_toggle")
                            ) {
                                ChevronIcon(
                                    isExpanded = isScientificExpanded,
                                    color = if (isDark) Color(0xFFD6C4FF) else Color(0xFF4C3E68),
                                    rotationDegrees = chevronRotation
                                )
                            }

                            if (isRadMode) {
                                Text(
                                    text = "RAD",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                    )
                                )
                            }
                        }

                // 4. Keyboard Section: Responsive aspect ratio prevents scientific keypad from squishing the display
                val buttonAspectRatio by keypadTransition.animateFloat(
                    transitionSpec = {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    },
                    label = "aspectRatioAnim"
                ) { expanded -> if (expanded && isPortrait) 1.85f else 1.05f }
                val keypadSpacing by keypadTransition.animateDp(
                    transitionSpec = {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    },
                    label = "keypadSpacingAnim"
                ) { expanded -> if (expanded && isPortrait) 6.dp else 12.dp }
                val keypadPaddingTop by keypadTransition.animateDp(
                    transitionSpec = {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    },
                    label = "paddingTopAnim"
                ) { expanded -> if (expanded && isPortrait) 8.dp else 18.dp }
                val keypadPaddingBottom by keypadTransition.animateDp(
                    transitionSpec = {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    },
                    label = "paddingBottomAnim"
                ) { expanded -> if (expanded && isPortrait) 12.dp else 22.dp }
                val scientificSpacing by keypadTransition.animateDp(
                    transitionSpec = {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    },
                    label = "scientificSpacingAnim"
                ) { expanded -> if (expanded && isPortrait) 6.dp else 10.dp }

                CompositionLocalProvider(LocalButtonAspectRatio provides buttonAspectRatio) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (collapsedKeypadHeightPx > 0) {
                                    Modifier.height(with(density) { collapsedKeypadHeightPx.toDp() })
                                } else {
                                    Modifier
                                }
                            )
                            .onSizeChanged { size ->
                                if (!isScientificExpanded && collapsedKeypadHeightPx == 0) {
                                    collapsedKeypadHeightPx = size.height
                                }
                            }
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(keypadBgColor)
                            .padding(start = 16.dp, end = 16.dp, top = keypadPaddingTop, bottom = keypadPaddingBottom),
                        verticalArrangement = if (collapsedKeypadHeightPx > 0) {
                            Arrangement.SpaceBetween
                        } else {
                            Arrangement.spacedBy(keypadSpacing)
                        }
                    ) {
                        // Expanded scientific keypad with subtle entry/exit animations
                        AnimatedVisibility(
                            visible = isScientificExpanded,
                            enter = expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                expandFrom = Alignment.Top
                            ),
                            exit = shrinkVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                shrinkTowards = Alignment.Top
                            )
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(scientificSpacing),
                                modifier = Modifier.padding(bottom = scientificSpacing)
                            ) {
                                // Row 1: √, π, ^, !
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(keypadSpacing)
                                ) {
                                    KeypadButton(
                                        label = "√",
                                        bgColor = scientificBtnBg,
                                        txtColor = scientificBtnTxt,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.handleKeyPress("√") }

                                    KeypadButton(
                                        label = "π",
                                        bgColor = scientificBtnBg,
                                        txtColor = scientificBtnTxt,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.handleKeyPress("π") }

                                    KeypadButton(
                                        label = "^",
                                        bgColor = scientificBtnBg,
                                        txtColor = scientificBtnTxt,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.handleKeyPress("^") }

                                    KeypadButton(
                                        label = "!",
                                        bgColor = scientificBtnBg,
                                        txtColor = scientificBtnTxt,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.handleKeyPress("!") }
                                }

                                // Row 2: RadToggle, sin, cos, tan
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(keypadSpacing)
                                ) {
                                    KeypadButton(
                                        label = if (isRadMode) "Rad" else "Deg",
                                        bgColor = if (isRadMode) (if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)) else scientificBtnBg,
                                        txtColor = if (isRadMode) (if (isDark) Color(0xFF381E72) else Color(0xFFFFFFFF)) else scientificBtnTxt,
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold
                                    ) { viewModel.toggleRadMode() }

                                    KeypadButton(
                                        label = if (isInvMode) "sin⁻¹" else "sin",
                                        bgColor = scientificBtnBg,
                                        txtColor = scientificBtnTxt,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.handleKeyPress("sin") }

                                    KeypadButton(
                                        label = if (isInvMode) "cos⁻¹" else "cos",
                                        bgColor = scientificBtnBg,
                                        txtColor = scientificBtnTxt,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.handleKeyPress("cos") }

                                    KeypadButton(
                                        label = if (isInvMode) "tan⁻¹" else "tan",
                                        bgColor = scientificBtnBg,
                                        txtColor = scientificBtnTxt,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.handleKeyPress("tan") }
                                }

                                // Row 3: Inv, e, ln, log
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(keypadSpacing)
                                ) {
                                    KeypadButton(
                                        label = "Inv",
                                        bgColor = if (isInvMode) (if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)) else scientificBtnBg,
                                        txtColor = if (isInvMode) (if (isDark) Color(0xFF381E72) else Color(0xFFFFFFFF)) else scientificBtnTxt,
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold
                                    ) { viewModel.toggleInvMode() }

                                    KeypadButton(
                                        label = "e",
                                        bgColor = scientificBtnBg,
                                        txtColor = scientificBtnTxt,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.handleKeyPress("e") }

                                    KeypadButton(
                                        label = "ln",
                                        bgColor = scientificBtnBg,
                                        txtColor = scientificBtnTxt,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.handleKeyPress("ln") }

                                    KeypadButton(
                                        label = "log",
                                        bgColor = scientificBtnBg,
                                        txtColor = scientificBtnTxt,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.handleKeyPress("log") }
                                }
                            }
                        }

                        // Collapsed key rows
                        // Row 1: AC, ( ), %, ÷
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(keypadSpacing)
                        ) {
                            KeypadButton(
                                label = "AC",
                                bgColor = if (isDark) Color(0xFF493E5C) else Color(0xFFFCE8E6),
                                txtColor = if (isDark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold
                            ) { viewModel.handleKeyPress("AC") }

                            KeypadButton(
                                label = "( )",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("( )") }

                            KeypadButton(
                                label = "%",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("%") }

                            KeypadButton(
                                label = "÷",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("÷") }
                        }

                        // Row 2: 7, 8, 9, ×
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(keypadSpacing)
                        ) {
                            KeypadButton(
                                label = "7",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("7") }

                            KeypadButton(
                                label = "8",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("8") }

                            KeypadButton(
                                label = "9",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("9") }

                            KeypadButton(
                                label = "×",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("×") }
                        }

                        // Row 3: 4, 5, 6, -
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(keypadSpacing)
                        ) {
                            KeypadButton(
                                label = "4",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("4") }

                            KeypadButton(
                                label = "5",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("5") }

                            KeypadButton(
                                label = "6",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("6") }

                            KeypadButton(
                                label = "-",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("-") }
                        }

                        // Row 4: 1, 2, 3, +
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(keypadSpacing)
                        ) {
                            KeypadButton(
                                label = "1",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("1") }

                            KeypadButton(
                                label = "2",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("2") }

                            KeypadButton(
                                label = "3",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("3") }

                            KeypadButton(
                                label = "+",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("+") }
                        }

                        // Row 5: 0, ., backspace, =
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(keypadSpacing)
                        ) {
                            KeypadButton(
                                label = "0",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress("0") }

                            KeypadButton(
                                label = ".",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.handleKeyPress(".") }

                            // Beautiful interactive backspace
                            val backspaceInteraction = remember { MutableInteractionSource() }
                            val isBackspacePressed by backspaceInteraction.collectIsPressedAsState()
                            val backspaceScale by animateFloatAsState(
                                targetValue = if (isBackspacePressed) 0.90f else 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.5f,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "backspaceScale"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(buttonAspectRatio)
                                    .graphicsLayer {
                                        scaleX = backspaceScale
                                        scaleY = backspaceScale
                                    }
                                    .clip(CircleShape)
                                    .background(operatorBtnBg)
                                    .clickable(
                                        interactionSource = backspaceInteraction,
                                        indication = ripple(bounded = true)
                                    ) {
                                        vibrate(false)
                                        viewModel.handleKeyPress("⌫")
                                    }
                                    .semantics { contentDescription = "Backspace" }
                                    .testTag("btn_backspace"),
                                contentAlignment = Alignment.Center
                            ) {
                                BackspaceIcon(color = operatorBtnTxt)
                            }

                            KeypadButton(
                                label = "=",
                                bgColor = equalsBtnBg,
                                txtColor = equalsBtnTxt,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold
                            ) { viewModel.handleKeyPress("=") }
                        }
                    }
                }
            }
        } else {
            // Dedicated Landscape Calculator View
            LandscapeCalculatorLayout(
                viewModel = viewModel,
                input = input,
                result = result,
                isRadMode = isRadMode,
                isInvMode = isInvMode,
                isDark = isDark,
                displayBgColor = displayBgColor,
                keypadBgColor = keypadBgColor,
                numberBtnBg = numberBtnBg,
                numberBtnTxt = numberBtnTxt,
                operatorBtnBg = operatorBtnBg,
                operatorBtnTxt = operatorBtnTxt,
                scientificBtnBg = scientificBtnBg,
                scientificBtnTxt = scientificBtnTxt,
                equalsBtnBg = equalsBtnBg,
                equalsBtnTxt = equalsBtnTxt
            )
        }
    }
}
}
}
}

        if (showReportDialog) {
            ReportIssueDialog(
                viewModel = viewModel,
                onDismiss = { showReportDialog = false },
                isDark = isDark
            )
        }
    }

@Composable
fun InlineHistoryRow(
    item: HistoryEntry,
    onSelect: (String) -> Unit,
    onDelete: (Long) -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelect(item.expression) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onDelete(item.id) },
            modifier = Modifier.size(24.dp)
        ) {
            TrashIcon(
                color = if (isDark) Color(0x66E6E1E5) else Color(0x661C1B1F),
                modifier = Modifier.size(14.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${item.expression}= ${item.result}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                ),
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TrashIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // Draw top handle/lid accent of trash can
        drawLine(
            color = color,
            start = Offset(w * 0.35f, h * 0.18f),
            end = Offset(w * 0.65f, h * 0.18f),
            strokeWidth = 2.dp.toPx()
        )
        // Draw lid horizontal bar
        drawLine(
            color = color,
            start = Offset(w * 0.16f, h * 0.26f),
            end = Offset(w * 0.84f, h * 0.26f),
            strokeWidth = 2.dp.toPx()
        )
        // Draw main body of bin
        val path = Path().apply {
            moveTo(w * 0.24f, h * 0.26f)
            lineTo(w * 0.30f, h * 0.85f)
            lineTo(w * 0.70f, h * 0.85f)
            lineTo(w * 0.76f, h * 0.26f)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
        // Inner vertical garbage stripes
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.36f),
            end = Offset(w * 0.42f, h * 0.75f),
            strokeWidth = 1.6.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(w * 0.58f, h * 0.36f),
            end = Offset(w * 0.58f, h * 0.75f),
            strokeWidth = 1.6.dp.toPx()
        )
    }
}

@Composable
fun HistoryIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.width / 2.4f
        drawCircle(
            color = color,
            radius = r,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - r * 0.55f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + r * 0.45f, center.y),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun ThreeDotMenuIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val radius = 2.dp.toPx()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        // Top Dot
        drawCircle(color = color, radius = radius, center = Offset(centerX, centerY - 6.dp.toPx()))
        // Center Dot
        drawCircle(color = color, radius = radius, center = Offset(centerX, centerY))
        // Bottom Dot
        drawCircle(color = color, radius = radius, center = Offset(centerX, centerY + 6.dp.toPx()))
    }
}

@Composable
fun LightModeIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        
        // Center sun body
        drawCircle(color = color, radius = 5.dp.toPx(), center = center, style = Stroke(width = 2.dp.toPx()))
        
        // 8 radiating sunburst ticks
        val numTicks = 8
        val innerF = 8.dp.toPx()
        val outerF = 11.dp.toPx()
        for (i in 0 until numTicks) {
            val angle = i * (2 * Math.PI / numTicks)
            val dx = Math.cos(angle).toFloat()
            val dy = Math.sin(angle).toFloat()
            drawLine(
                color = color,
                start = Offset(center.x + dx * innerF, center.y + dy * innerF),
                end = Offset(center.x + dx * outerF, center.y + dy * outerF),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
fun DarkModeIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            cubicTo(
                w * 0.15f, h * 0.25f,
                w * 0.15f, h * 0.75f,
                w * 0.5f, h * 0.85f
            )
            cubicTo(
                w * 0.35f, h * 0.75f,
                w * 0.35f, h * 0.25f,
                w * 0.5f, h * 0.15f
            )
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
fun SystemModeIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.width / 3f
        
        // Draw outline of the circle
        drawCircle(
            color = color,
            radius = r,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Fill the left half
        val path = Path().apply {
            addArc(
                oval = androidx.compose.ui.geometry.Rect(
                    center.x - r,
                    center.y - r,
                    center.x + r,
                    center.y + r
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f
            )
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
fun ChevronIcon(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    color: Color,
    rotationDegrees: Float? = null
) {
    val animatedRotation = if (rotationDegrees == null) {
        val rotation by animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "chevronRotation"
        )
        rotation
    } else {
        rotationDegrees
    }
    Canvas(
        modifier = modifier
            .size(16.dp)
            .graphicsLayer { rotationZ = animatedRotation }
    ) {
        val path = Path()
        val w = size.width
        val h = size.height
        // Arrow pointing downwards initially, rotated by rotationZ
        path.moveTo(w * 0.15f, h * 0.35f)
        path.lineTo(w * 0.5f, h * 0.68f)
        path.lineTo(w * 0.85f, h * 0.35f)
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun BackspaceIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.5f)
            lineTo(w * 0.38f, h * 0.2f)
            lineTo(w * 0.88f, h * 0.2f)
            lineTo(w * 0.88f, h * 0.8f)
            lineTo(w * 0.38f, h * 0.8f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
        // Inner crossing line representing delete label
        drawLine(
            color = color,
            start = Offset(w * 0.48f, h * 0.38f),
            end = Offset(w * 0.72f, h * 0.62f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(w * 0.72f, h * 0.38f),
            end = Offset(w * 0.48f, h * 0.62f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun rememberVibrator(): (isSpecial: Boolean) -> Unit {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    return remember(vibrator, haptic) {
        { isSpecial ->
            try {
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val effect = if (isSpecial) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                        } else {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        }
                        vibrator.vibrate(effect)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                if (isSpecial) 35L else 18L,
                                if (isSpecial) VibrationEffect.DEFAULT_AMPLITUDE else 120
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(if (isSpecial) 35L else 18L)
                    }
                } else {
                    haptic.performHapticFeedback(
                        if (isSpecial) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                    )
                }
            } catch (_: Exception) {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) { }
            }
        }
    }
}

@Composable
fun KeypadButton(
    label: String,
    bgColor: Color,
    txtColor: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Medium,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    onClick: () -> Unit
) {
    val buttonAspectRatio = LocalButtonAspectRatio.current
    val vibrate = rememberVibrator()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 450f
        ),
        label = "btnScale"
    )

    val animatedBg by animateColorAsState(
        targetValue = if (isPressed) bgColor.copy(alpha = 0.82f) else bgColor,
        animationSpec = tween(durationMillis = 100),
        label = "btnColorAnim"
    )

    val sizingModifier = when {
        buttonAspectRatio > 0f -> modifier.aspectRatio(buttonAspectRatio)
        buttonAspectRatio == 0f -> modifier.fillMaxHeight()
        else -> modifier
    }

    Box(
        modifier = sizingModifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(animatedBg)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true)
            ) {
                val isHeavy = label == "=" || label == "AC" || label == "C"
                vibrate(isHeavy)
                onClick()
            }
            .semantics {
                contentDescription = when (label) {
                    "AC" -> "Clear all"
                    "C" -> "Clear"
                    "⌫" -> "Backspace"
                    "=" -> "Equals"
                    "+" -> "Plus"
                    "-" -> "Minus"
                    "×" -> "Multiply"
                    "÷" -> "Divide"
                    "^" -> "Power"
                    "√" -> "Square root"
                    "π" -> "Pi"
                    "%" -> "Percentage"
                    "!" -> "Factorial"
                    else -> label
                }
            }
            .testTag("btn_$label"),
        contentAlignment = Alignment.Center
    ) {
        val effectiveFontSize = remember(fontSize, buttonAspectRatio, label) {
            if (buttonAspectRatio > 1.3f) {
                if (label.length >= 4) 11.sp
                else if (label.length >= 3) 13.sp
                else 16.sp
            } else {
                if (label.length >= 4) 13.sp
                else if (label.length >= 3) 16.sp
                else fontSize
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = effectiveFontSize,
                color = txtColor,
                fontWeight = fontWeight
            )
        )
    }
}

@Composable
fun HistoryRow(
    item: HistoryEntry,
    onSelect: (String) -> Unit,
    onDelete: (Long) -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF232027) else Color(0xFFF1EEF4))
            .clickable { onSelect(item.expression) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.expression,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isDark) Color(0xFFCCC2DC) else Color(0xFF49454F)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "= ${item.result}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
                )
            )
        }
        
        IconButton(
            onClick = { onDelete(item.id) }
        ) {
            TrashIcon(
                color = if (isDark) Color(0x66E6E1E5) else Color(0x661C1B1F),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Dynamically adjusts input text size based on formulas length to avoid clipping
@Composable
private fun TextStyleForInput(input: String) = when {
    input.length < 8 -> MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold)
    input.length < 14 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold)
    input.length < 22 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal)
    input.length < 32 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
    else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal)
}

// Dynamically adjusts live output result size as well to make everything beautifully fit
@Composable
private fun TextStyleForOutput(result: String) = when {
    result.length < 10 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium)
    result.length < 16 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
    result.length < 24 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
    else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
}

// Dynamically adjusts landscape input text size to guarantee no clipping or overlap
@Composable
private fun LandscapeTextStyleForInput(input: String) = when {
    input.length < 8 -> MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold)
    input.length < 14 -> MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold)
    input.length < 22 -> MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal)
    input.length < 32 -> MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)
    else -> MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
}

// Dynamically adjusts landscape output preview size
@Composable
private fun LandscapeTextStyleForOutput(result: String) = when {
    result.length < 10 -> MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    result.length < 16 -> MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)
    result.length < 24 -> MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    else -> MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun LandscapeCalculatorLayout(
    viewModel: CalculatorViewModel,
    input: String,
    result: String,
    isRadMode: Boolean,
    isInvMode: Boolean,
    isDark: Boolean,
    displayBgColor: Color,
    keypadBgColor: Color,
    numberBtnBg: Color,
    numberBtnTxt: Color,
    operatorBtnBg: Color,
    operatorBtnTxt: Color,
    scientificBtnBg: Color,
    scientificBtnTxt: Color,
    equalsBtnBg: Color,
    equalsBtnTxt: Color
) {
    val vibrate = rememberVibrator()
    val activeSciBg = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    val activeSciTxt = if (isDark) Color(0xFF381E72) else Color(0xFFFFFFFF)
    val alertBg = if (isDark) Color(0xFF493E5C) else Color(0xFFFCE8E6)
    val alertTxt = if (isDark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)

    var isLandScientificExpanded by rememberSaveable { mutableStateOf(true) }
    val sciWeight by animateFloatAsState(
        targetValue = if (isLandScientificExpanded) 3f else 0.0001f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "sciWeight"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top ~34% Screen Height: Dedicated Slim Horizontal Display Panel with guaranteed headroom
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.34f),
            shape = RoundedCornerShape(16.dp),
            color = displayBgColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Toggle Scientific / Basic Pill Button & RAD Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            vibrate(false)
                            isLandScientificExpanded = !isLandScientificExpanded
                        },
                        shape = CircleShape,
                        color = if (isLandScientificExpanded) {
                            if (isDark) Color(0xFF381E72) else Color(0xFFEADDFF)
                        } else {
                            if (isDark) Color(0xFF2B2930) else Color(0xFFF3EDF7)
                        },
                        modifier = Modifier
                            .semantics { contentDescription = if (isLandScientificExpanded) "Collapse scientific keypad" else "Expand scientific keypad" }
                            .testTag("land_scientific_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ChevronIcon(
                                isExpanded = isLandScientificExpanded,
                                color = if (isDark) Color(0xFFD6C4FF) else Color(0xFF4C3E68)
                            )
                            Text(
                                text = if (isLandScientificExpanded) "Scientific" else "Basic",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFD6C4FF) else Color(0xFF4C3E68)
                                )
                            )
                        }
                    }

                    if (isRadMode) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF381E72) else Color(0xFFEADDFF)
                        ) {
                            Text(
                                text = "RAD",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                )
                            )
                        }
                    }
                }

                // Right Side: Single-Line Horizontally Scrollable Expression & Preview
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    val landInputScrollState = rememberScrollState()
                    LaunchedEffect(input) {
                        if (input.isNotEmpty()) {
                            landInputScrollState.animateScrollTo(landInputScrollState.maxValue)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(landInputScrollState),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = if (input.isEmpty()) "0" else input,
                            style = LandscapeTextStyleForInput(input),
                            color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag("calculation_input"),
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    val landPreviewScrollState = rememberScrollState()
                    LaunchedEffect(result) {
                        if (result.isNotEmpty()) {
                            landPreviewScrollState.animateScrollTo(landPreviewScrollState.maxValue)
                        }
                    }

                    AnimatedContent(
                        targetState = result,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                slideInVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) { it / 3 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing))
                                )
                        },
                        label = "landCalculationPreviewAnim"
                    ) { previewText ->
                        if (previewText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(landPreviewScrollState),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = previewText,
                                    style = LandscapeTextStyleForOutput(previewText),
                                    color = if (isDark) Color(0xFFD6C4FF) else Color(0xFF6750A4),
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.testTag("calculation_preview"),
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom ~66% Screen Height: Keypad Container (3 + 4 Architecture with Oval Keys)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.66f),
            shape = RoundedCornerShape(16.dp),
            color = keypadBgColor
        ) {
            CompositionLocalProvider(LocalButtonAspectRatio provides -1f) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val btnShape = CircleShape
                    val btnHeight = 38.dp
                    val sciFontSize = 13.sp
                    val numFontSize = 16.sp

                    // Left Side: 3 Columns Scientific Functions (Collapsible)
                    if (sciWeight > 0.05f) {
                        Column(
                            modifier = Modifier
                                .weight(sciWeight)
                                .fillMaxHeight()
                                .clipToBounds()
                                .graphicsLayer {
                                    alpha = (sciWeight / 3f).coerceIn(0f, 1f)
                                },
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Row 1: [DEG/RAD Toggle] [√] [π]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KeypadButton(
                                    label = if (isRadMode) "RAD" else "DEG",
                                    bgColor = if (isRadMode) activeSciBg else scientificBtnBg,
                                    txtColor = if (isRadMode) activeSciTxt else scientificBtnTxt,
                                    modifier = Modifier.weight(1f).height(btnHeight),
                                    fontWeight = FontWeight.Bold,
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.toggleRadMode() }

                                KeypadButton("√", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("√") }
                                KeypadButton("π", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("π") }
                            }

                            // Row 2: [sin/sin⁻¹] [cos/cos⁻¹] [tan/tan⁻¹]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KeypadButton(if (isInvMode) "sin⁻¹" else "sin", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = 12.sp) { viewModel.handleKeyPress("sin") }
                                KeypadButton(if (isInvMode) "cos⁻¹" else "cos", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = 12.sp) { viewModel.handleKeyPress("cos") }
                                KeypadButton(if (isInvMode) "tan⁻¹" else "tan", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = 12.sp) { viewModel.handleKeyPress("tan") }
                            }

                            // Row 3: [ln] [log] [^]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KeypadButton("ln", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("ln") }
                                KeypadButton("log", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("log") }
                                KeypadButton("^", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("^") }
                            }

                            // Row 4: [e] [!] [Inv]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KeypadButton("e", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("e") }
                                KeypadButton("!", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("!") }
                                KeypadButton(
                                    label = "Inv",
                                    bgColor = if (isInvMode) activeSciBg else scientificBtnBg,
                                    txtColor = if (isInvMode) activeSciTxt else scientificBtnTxt,
                                    modifier = Modifier.weight(1f).height(btnHeight),
                                    fontWeight = FontWeight.Bold,
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.toggleInvMode() }
                            }

                            // Row 5: [(] [)]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KeypadButton("(", scientificBtnBg, scientificBtnTxt, Modifier.weight(1.5f).height(btnHeight), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("(") }
                                KeypadButton(")", scientificBtnBg, scientificBtnTxt, Modifier.weight(1.5f).height(btnHeight), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress(")") }
                            }
                        }
                    }

                    // Right Side: 4 Columns Primary Numeric & Operator Keypad
                    Column(
                        modifier = Modifier
                            .weight(4f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Row 1: [AC (2f)] [% (1f)] [÷ (1f)]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeypadButton("AC", alertBg, alertTxt, Modifier.weight(2f).height(btnHeight), fontWeight = FontWeight.Bold, shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("AC") }
                            KeypadButton("%", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("%") }
                            KeypadButton("÷", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("÷") }
                        }

                        // Row 2: [7] [8] [9] [×]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeypadButton("7", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("7") }
                            KeypadButton("8", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("8") }
                            KeypadButton("9", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("9") }
                            KeypadButton("×", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("×") }
                        }

                        // Row 3: [4] [5] [6] [-]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeypadButton("4", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("4") }
                            KeypadButton("5", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("5") }
                            KeypadButton("6", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("6") }
                            KeypadButton("-", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("-") }
                        }

                        // Row 4: [1] [2] [3] [+]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeypadButton("1", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("1") }
                            KeypadButton("2", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("2") }
                            KeypadButton("3", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("3") }
                            KeypadButton("+", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f).height(btnHeight), shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("+") }
                        }

                        // Row 5: [0] [.] [⌫] [=]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeypadButton("0", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("0") }
                            KeypadButton(".", numberBtnBg, numberBtnTxt, Modifier.weight(1f).height(btnHeight), fontWeight = FontWeight.Bold, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress(".") }

                            // Backspace interactive button
                            val backspaceInteraction = remember { MutableInteractionSource() }
                            val isPressed by backspaceInteraction.collectIsPressedAsState()
                            val backspaceScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.90f else 1f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                                label = "landBackspaceScale"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(btnHeight)
                                    .graphicsLayer {
                                        scaleX = backspaceScale
                                        scaleY = backspaceScale
                                    }
                                    .clip(btnShape)
                                    .background(operatorBtnBg)
                                    .clickable(
                                        interactionSource = backspaceInteraction,
                                        indication = ripple(bounded = true)
                                    ) {
                                        vibrate(false)
                                        viewModel.handleKeyPress("⌫")
                                    }
                                    .semantics { contentDescription = "Backspace" }
                                    .testTag("btn_backspace"),
                                contentAlignment = Alignment.Center
                            ) {
                                BackspaceIcon(color = operatorBtnTxt)
                            }

                            // Equals button (aligned in numeric operator column under +)
                            KeypadButton(
                                label = "=",
                                bgColor = equalsBtnBg,
                                txtColor = equalsBtnTxt,
                                modifier = Modifier.weight(1f).height(btnHeight),
                                fontWeight = FontWeight.Bold,
                                shape = btnShape,
                                fontSize = 20.sp
                            ) { viewModel.handleKeyPress("=") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DropMenuItemScreenshotBlock(
    isScreenshotBlocked: Boolean,
    onToggle: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Block Screenshots")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isScreenshotBlocked) Color(0xFF2E7D32) else Color(0xFF757575)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isScreenshotBlocked) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        },
        onClick = onToggle,
        modifier = Modifier.testTag("menu_block_screenshots")
    )
}


