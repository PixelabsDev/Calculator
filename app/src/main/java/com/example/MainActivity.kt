package com.example

import android.content.Context
import android.content.res.Configuration
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
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
import com.example.integrity.DeviceSecurityReport
import com.example.report.ReportIssueDialog
import com.example.ui.theme.MyApplicationTheme

val LocalButtonAspectRatio = compositionLocalOf { 1f }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    val securityReport by viewModel.deviceSecurityReport.collectAsState()
    val isCheckingIntegrity by viewModel.isCheckingIntegrity.collectAsState()
    val isScreenshotBlocked by viewModel.isScreenshotBlocked.collectAsState()

    var showHistoryDrawer by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showIntegrityDialog by remember { mutableStateOf(false) }
    var showPrivacyScreen by remember { mutableStateOf(false) }
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
    val keypadBgColor = if (isDark) Color(0xFF1B1824) else Color(0xFFEDE8F5)

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

    if (showPrivacyScreen) {
        PrivacyAndTermsScreen(
            onBack = { showPrivacyScreen = false },
            isDark = isDark
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBgColor)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                val vibrate = rememberVibrator()

                // 1. Top Bar Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            vibrate(false)
                            showHistoryDrawer = !showHistoryDrawer
                        },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        HistoryIcon(color = iconColor)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleThemeMode() },
                            modifier = Modifier.testTag("theme_toggle_button")
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
                                modifier = Modifier.testTag("more_options_button")
                            ) {
                                ThreeDotMenuIcon(color = iconColor)
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Privacy Policy & Terms") },
                                    onClick = {
                                        showPrivacyScreen = true
                                        showMenu = false
                                    },
                                    modifier = Modifier.testTag("menu_privacy_terms")
                                )
                                DropdownMenuItem(
                                    text = { Text("Device Integrity & Security") },
                                    onClick = {
                                        viewModel.checkDeviceIntegrity()
                                        showIntegrityDialog = true
                                        showMenu = false
                                    },
                                    modifier = Modifier.testTag("menu_device_integrity")
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
                                DropdownMenuItem(
                                    text = {
                                        val mStr = when (themeMode) {
                                            com.example.calculator.ThemeMode.SYSTEM -> "Use System Theme"
                                            com.example.calculator.ThemeMode.LIGHT -> "Force Light Theme"
                                            com.example.calculator.ThemeMode.DARK -> "Force Dark Theme"
                                        }
                                        Text(mStr)
                                    },
                                    onClick = {
                                        viewModel.toggleThemeMode()
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
                                    .testTag("scientific_toggle")
                            ) {
                                ChevronIcon(
                                    isExpanded = isScientificExpanded,
                                    color = if (isDark) Color(0xFFD6C4FF) else Color(0xFF4C3E68)
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
                val targetAspectRatio = if (isScientificExpanded && isPortrait) 1.85f else 1.05f
                val targetKeypadSpacing = if (isScientificExpanded && isPortrait) 6.dp else 12.dp
                val targetKeypadPaddingTop = if (isScientificExpanded && isPortrait) 8.dp else 18.dp
                val targetKeypadPaddingBottom = if (isScientificExpanded && isPortrait) 12.dp else 22.dp
                val targetScientificSpacing = if (isScientificExpanded && isPortrait) 6.dp else 10.dp

                val animSpecFloat = tween<Float>(durationMillis = 260, easing = FastOutSlowInEasing)
                val animSpecDp = tween<androidx.compose.ui.unit.Dp>(durationMillis = 260, easing = FastOutSlowInEasing)

                val buttonAspectRatio by animateFloatAsState(
                    targetValue = targetAspectRatio,
                    animationSpec = animSpecFloat,
                    label = "aspectRatioAnim"
                )
                val keypadSpacing by animateDpAsState(
                    targetValue = targetKeypadSpacing,
                    animationSpec = animSpecDp,
                    label = "keypadSpacingAnim"
                )
                val keypadPaddingTop by animateDpAsState(
                    targetValue = targetKeypadPaddingTop,
                    animationSpec = animSpecDp,
                    label = "paddingTopAnim"
                )
                val keypadPaddingBottom by animateDpAsState(
                    targetValue = targetKeypadPaddingBottom,
                    animationSpec = animSpecDp,
                    label = "paddingBottomAnim"
                )
                val scientificSpacing by animateDpAsState(
                    targetValue = targetScientificSpacing,
                    animationSpec = animSpecDp,
                    label = "scientificSpacingAnim"
                )

                CompositionLocalProvider(LocalButtonAspectRatio provides buttonAspectRatio) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(keypadBgColor)
                            .padding(start = 16.dp, end = 16.dp, top = keypadPaddingTop, bottom = keypadPaddingBottom),
                        verticalArrangement = Arrangement.spacedBy(keypadSpacing)
                    ) {
                        // Expanded scientific keypad with subtle entry/exit animations
                        AnimatedVisibility(
                            visible = isScientificExpanded,
                            enter = expandVertically(
                                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                                expandFrom = Alignment.Top
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                            ),
                            exit = shrinkVertically(
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                shrinkTowards = Alignment.Top
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
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

        if (showIntegrityDialog) {
            DeviceIntegrityDialog(
                viewModel = viewModel,
                report = securityReport,
                isChecking = isCheckingIntegrity,
                onDismiss = { showIntegrityDialog = false },
                isDark = isDark
            )
        }

        if (showReportDialog) {
            ReportIssueDialog(
                viewModel = viewModel,
                onDismiss = { showReportDialog = false },
                isDark = isDark
            )
        }
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
fun ChevronIcon(isExpanded: Boolean, modifier: Modifier = Modifier, color: Color) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "chevronRotation"
    )
    Canvas(
        modifier = modifier
            .size(16.dp)
            .graphicsLayer { rotationZ = rotation }
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

    val sizingModifier = if (buttonAspectRatio > 0f) {
        modifier.aspectRatio(buttonAspectRatio)
    } else {
        modifier.fillMaxHeight()
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

// Adjusts live output result size as well to make everything beautifully fit
@Composable
private fun TextStyleForOutput(result: String) = when {
    result.length < 10 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium)
    result.length < 16 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
    result.length < 24 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
    else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
}

@Composable
fun DeviceIntegrityDialog(
    viewModel: CalculatorViewModel,
    report: DeviceSecurityReport?,
    isChecking: Boolean,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_device_integrity"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecurityShieldIcon(modifier = Modifier.size(24.dp), color = if (report?.isDeviceCertified == true) Color(0xFF4CAF50) else Color(0xFFFFA000))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Play Integrity & Device Security",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isChecking) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text(
                                text = "Evaluating Play Integrity & Device Certification...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else if (report != null) {
                    // Status Badge Banner
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (report.isDeviceCertified) (if (isDark) Color(0xFF1B2E1E) else Color(0xFFE8F5E9)) else (if (isDark) Color(0xFF332A18) else Color(0xFFFFF8E1))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CheckCircleBadgeIcon(
                                    modifier = Modifier.size(18.dp),
                                    color = if (report.isDeviceCertified) Color(0xFF4CAF50) else Color(0xFFFFA000)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (report.isDeviceCertified) "Google Play Certified Device" else "Device Verification Notice",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (report.isDeviceCertified) (if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)) else (if (isDark) Color(0xFFFFD54F) else Color(0xFFF57F17))
                                    )
                                )
                            }
                            Text(
                                text = report.certificationSummary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (report.isDeviceCertified) (if (isDark) Color(0xFFC8E6C9) else Color(0xFF388E3C)) else (if (isDark) Color(0xFFFFE082) else Color(0xFFE65100))
                                )
                            )
                        }
                    }

                    // Diagnostic Details List
                    IntegrityDetailRow("Google Play Services", "${report.playServicesStatus} (${report.playServicesVersion})", isDark)
                    IntegrityDetailRow("Play Integrity Token", report.integrityTokenStatus, isDark)
                    IntegrityDetailRow("Device Model", "${report.manufacturer} ${report.deviceModel}", isDark)
                    IntegrityDetailRow("OS & SDK Version", "${report.androidVersion} (API ${report.apiLevel})", isDark)
                    IntegrityDetailRow("Security Patch", report.securityPatch, isDark)
                    IntegrityDetailRow("Build Signature", if (report.isReleaseKeys) "Official Release Keys" else report.buildTags, isDark)
                    IntegrityDetailRow("Tampering / Root Status", if (report.isRootDetected) "Root Binary Detected" else "Clean (No Tampering)", isDark)
                    IntegrityDetailRow("Last Verified", report.timestamp, isDark)
                } else {
                    Text(
                        text = "Press verify below to evaluate Google Play Integrity and device certification.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.checkDeviceIntegrity() },
                enabled = !isChecking,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_verify_integrity_again")
            ) {
                Text(if (isChecking) "Checking..." else "Re-verify")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun IntegrityDetailRow(label: String, value: String, isDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF221F28) else Color(0xFFF3EFF6))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isDark) Color(0xFF9E9B9F) else Color(0xFF7D7A7F),
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F),
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun SecurityShieldIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            lineTo(w * 0.85f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.6f)
            cubicTo(
                w * 0.85f, h * 0.85f,
                w * 0.5f, h * 0.95f,
                w * 0.5f, h * 0.95f
            )
            cubicTo(
                w * 0.5f, h * 0.95f,
                w * 0.15f, h * 0.85f,
                w * 0.15f, h * 0.6f
            )
            lineTo(w * 0.15f, h * 0.25f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
        // Inner checkmark in shield
        val checkPath = Path().apply {
            moveTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.48f, h * 0.63f)
            lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(path = checkPath, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CheckCircleBadgeIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = color, radius = w / 2.2f, center = Offset(w / 2f, h / 2f))
        val check = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.68f)
            lineTo(w * 0.74f, h * 0.35f)
        }
        drawPath(
            path = check,
            color = Color.White,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left Pane: Calculation Display Canvas
        Column(
            modifier = Modifier
                .weight(0.36f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(displayBgColor)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Indicator Badge (Clickable to toggle Rad / Deg)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRadMode) (if (isDark) Color(0xFF383048) else Color(0xFFE8DEF8)) else (if (isDark) Color(0xFF221E2C) else Color(0xFFF3EDF7)))
                        .clickable {
                            vibrate(false)
                            viewModel.toggleRadMode()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isRadMode) "RAD" else "DEG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFD6C4FF) else Color(0xFF6750A4)
                        )
                    )
                }
            }

            // Input Expression and Output Evaluation Result
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(landPreviewScrollState),
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
        }

        // Right Pane: Scientific & Numeric Keypad (5 equal rows x 7 columns)
        CompositionLocalProvider(LocalButtonAspectRatio provides 0f) {
            Column(
                modifier = Modifier
                    .weight(0.64f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(keypadBgColor)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val btnShape = RoundedCornerShape(10.dp)
                val sciFontSize = 15.sp
                val numFontSize = 17.sp

                // Row 1: [Rad/Deg] [√] [π] [AC] [( )] [%] [÷]
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeypadButton(
                        label = if (isRadMode) "Rad" else "Deg",
                        bgColor = if (isRadMode) activeSciBg else scientificBtnBg,
                        txtColor = if (isRadMode) activeSciTxt else scientificBtnTxt,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        shape = btnShape,
                        fontSize = sciFontSize
                    ) { viewModel.toggleRadMode() }

                    KeypadButton("√", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("√") }
                    KeypadButton("π", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("π") }
                    KeypadButton("AC", alertBg, alertTxt, Modifier.weight(1f), fontWeight = FontWeight.Bold, shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("AC") }
                    KeypadButton("( )", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("( )") }
                    KeypadButton("%", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("%") }
                    KeypadButton("÷", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("÷") }
                }

                // Row 2: [sin/sin⁻¹] [cos/cos⁻¹] [tan/tan⁻¹] [7] [8] [9] [×]
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeypadButton(if (isInvMode) "sin⁻¹" else "sin", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = 13.sp) { viewModel.handleKeyPress("sin") }
                    KeypadButton(if (isInvMode) "cos⁻¹" else "cos", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = 13.sp) { viewModel.handleKeyPress("cos") }
                    KeypadButton(if (isInvMode) "tan⁻¹" else "tan", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = 13.sp) { viewModel.handleKeyPress("tan") }
                    KeypadButton("7", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("7") }
                    KeypadButton("8", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("8") }
                    KeypadButton("9", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("9") }
                    KeypadButton("×", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("×") }
                }

                // Row 3: [ln] [log] [^] [4] [5] [6] [-]
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeypadButton("ln", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("ln") }
                    KeypadButton("log", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("log") }
                    KeypadButton("^", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("^") }
                    KeypadButton("4", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("4") }
                    KeypadButton("5", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("5") }
                    KeypadButton("6", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("6") }
                    KeypadButton("-", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("-") }
                }

                // Row 4: [e] [!] [Inv] [1] [2] [3] [+]
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeypadButton("e", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("e") }
                    KeypadButton("!", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("!") }
                    KeypadButton(
                        label = "Inv",
                        bgColor = if (isInvMode) activeSciBg else scientificBtnBg,
                        txtColor = if (isInvMode) activeSciTxt else scientificBtnTxt,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        shape = btnShape,
                        fontSize = sciFontSize
                    ) { viewModel.toggleInvMode() }

                    KeypadButton("1", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("1") }
                    KeypadButton("2", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("2") }
                    KeypadButton("3", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("3") }
                    KeypadButton("+", operatorBtnBg, operatorBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("+") }
                }

                // Row 5: [(] [)] [0] [.] [⌫] [= (weight 2f)]
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeypadButton("(", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress("(") }
                    KeypadButton(")", scientificBtnBg, scientificBtnTxt, Modifier.weight(1f), shape = btnShape, fontSize = sciFontSize) { viewModel.handleKeyPress(")") }
                    KeypadButton("0", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Medium, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress("0") }
                    KeypadButton(".", numberBtnBg, numberBtnTxt, Modifier.weight(1f), fontWeight = FontWeight.Bold, shape = btnShape, fontSize = numFontSize) { viewModel.handleKeyPress(".") }

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
                            .fillMaxHeight()
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
                            .testTag("btn_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        BackspaceIcon(color = operatorBtnTxt)
                    }

                    // Equals button (weight 2f for clear hit target)
                    KeypadButton(
                        label = "=",
                        bgColor = equalsBtnBg,
                        txtColor = equalsBtnTxt,
                        modifier = Modifier.weight(2f),
                        fontWeight = FontWeight.Bold,
                        shape = btnShape,
                        fontSize = 20.sp
                    ) { viewModel.handleKeyPress("=") }
                }
            }
        }
    }
}

@Composable
fun PrivacyAndTermsScreen(
    onBack: () -> Unit,
    isDark: Boolean
) {
    BackHandler(onBack = onBack)
    var selectedTab by remember { mutableStateOf(0) }

    val bgColor = if (isDark) Color(0xFF0F0E14) else Color(0xFFF9F6FC)
    val cardBg = if (isDark) Color(0xFF1B1824) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF2C2738) else Color(0xFFE8E0F0)
    val textColor = if (isDark) Color(0xFFF3EDFF) else Color(0xFF191622)
    val subtextColor = if (isDark) Color(0xFFA59EB5) else Color(0xFF6E6879)
    val accentColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    val onAccentColor = if (isDark) Color(0xFF381E72) else Color(0xFFFFFFFF)
    val iconColor = if (isDark) Color(0xFFF3EDFF) else Color(0xFF191622)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("screen_privacy_terms")
    ) {
        // 1. Top App Bar with back navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("privacy_back_button")
            ) {
                ArrowBackIcon(color = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Privacy & Legal Notice",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
                Text(
                    text = "Calculator by PixelabsDev • Official Terms",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = subtextColor
                    )
                )
            }
            IconButton(
                onClick = onBack
            ) {
                LegalPolicyIcon(modifier = Modifier.size(22.dp), color = accentColor)
            }
        }

        // 2. Segmented Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF231F2E) else Color(0xFFEDE5F4))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 0) accentColor else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) onAccentColor else (if (isDark) Color(0xFFCCC2DC) else Color(0xFF49454F))
                    )
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 1) accentColor else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Terms & Conditions",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) onAccentColor else (if (isDark) Color(0xFFCCC2DC) else Color(0xFF49454F))
                    )
                )
            }
        }

        // 3. Full-window scrollable legal content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overview Header Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecurityShieldIcon(
                            modifier = Modifier.size(28.dp),
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Calculator",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            )
                            Text(
                                text = "Developed & Published by PixelabsDev",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Text(
                        text = "Last updated: October 2026. This comprehensive legal document outlines our commitment to user privacy, offline local computing standards, and the terms of service governing this application.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = subtextColor,
                            lineHeight = 18.sp
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LegalBadge(text = "100% Offline", isDark = isDark)
                        LegalBadge(text = "No Ads", isDark = isDark)
                        LegalBadge(text = "Local Storage", isDark = isDark)
                    }
                }
            }

            if (selectedTab == 0) {
                // Privacy Policy Detailed Cards
                LegalSectionCard(
                    number = "1",
                    title = "Publisher Identity & Contact",
                    body = "This software is designed, engineered, and published by PixelabsDev (Pixelabs Digital Services). We treat user privacy with absolute priority. You can reach our technical and legal support team anytime at hello@pixelabs.in or via our website at https://pixelabs.in.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "2",
                    title = "Zero Math Telemetry & Offline Processing",
                    body = "All calculations—including basic arithmetic, scientific trigonometric formulas (sin, cos, tan), logarithmic functions (ln, log), powers, roots, and angle computations—are performed 100% on your device CPU. We never transmit, log, intercept, or upload your calculation formulas or numeric entries to any external servers.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "3",
                    title = "Local Database Persistence (Room)",
                    body = "Your calculation history is stored locally within an encrypted/sandboxed SQLite Room database on your device storage. You retain complete ownership and control over this data. You can delete individual calculation records or completely purge your calculation history at any time with a single tap.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "4",
                    title = "100% Offline Architecture & Zero Cloud Backup",
                    body = "This application operates 100% offline with zero cloud backup or remote account synchronization. All calculations and history entries remain exclusively on your device hardware, guaranteeing complete privacy and zero data collection.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "5",
                    title = "Device Integrity & Google Play Security",
                    body = "The app integrates Google Play Integrity API checks locally to verify that the app binary has not been modified or tampered with and is running in a genuine Android environment. This check is purely for application security and anti-tampering verification.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "6",
                    title = "Zero Advertising & No Data Selling",
                    body = "PixelabsDev does not monetize this application through advertising networks, trackers, data brokers, or behavioral analytics SDKs. We do not sell, rent, or trade any user data to third parties under any circumstances.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "7",
                    title = "System Permissions Explained",
                    body = "• VIBRATE: Required to provide tactile haptic feedback during keypad button presses for an authentic calculator feel (can be adjusted in device settings).\n• Offline Privacy: The app operates 100% offline with zero cloud backup, tracking, or network transmission of your personal calculation entries.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "8",
                    title = "Children & Student Privacy",
                    body = "This application is safe for users of all ages, including children and students. We do not knowingly collect personal data from minors, making it fully compliant with COPPA and GDPR regulations.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )
            } else {
                // Terms & Conditions Detailed Cards
                LegalSectionCard(
                    number = "1",
                    title = "Acceptance of Agreement",
                    body = "By downloading, installing, accessing, or using Calculator by PixelabsDev, you agree to comply with and be bound by these Terms and Conditions. If you disagree with any part of these terms, please discontinue use of the application.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "2",
                    title = "Software License & Permitted Use",
                    body = "PixelabsDev grants you a personal, non-transferable, non-exclusive, revocable license to use this calculator application for personal, educational, research, engineering, and commercial computational purposes in accordance with these terms.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "3",
                    title = "Mathematical Accuracy & Disclaimer",
                    body = "The application implements double-precision IEEE-754 floating-point computation algorithms. While designed to deliver high accuracy across arithmetic, trigonometric, and scientific functions, all calculation outputs are provided on an 'as-is' basis. Users are advised to independently verify critical architectural, medical, financial, or engineering calculations.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "4",
                    title = "Intellectual Property Rights",
                    body = "All algorithms, source code, user interface designs, custom graphic icons, typography pairings, and PixelabsDev brand assets are the exclusive intellectual property of PixelabsDev and protected under international copyright and intellectual property laws.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "5",
                    title = "Software Updates & Modifications",
                    body = "PixelabsDev reserves the right to periodically release software updates, enhance features, optimize performance, and adjust mathematical models to improve system stability and user experience.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "6",
                    title = "Limitation of Liability",
                    body = "To the maximum extent permitted by applicable law, PixelabsDev and its developers shall not be liable for any indirect, incidental, special, consequential, or punitive damages arising from the use or inability to use this software.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )

                LegalSectionCard(
                    number = "7",
                    title = "Governing Law & Inquiries",
                    body = "These terms shall be governed by and construed in accordance with applicable laws. For inquiries regarding licensing, enterprise usage, or terms clarification, please contact hello@pixelabs.in.",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor
                )
            }

            // Developer Contact Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF221E2C) else Color(0xFFF1EAF8)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Official Support & Inquiries",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                    Text(
                        text = "• Official Publisher: PixelabsDev\n• Support Email: hello@pixelabs.in\n• Official Website: https://pixelabs.in\n• Application: Calculator for Android",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = subtextColor,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            // Primary Back to Calculator Button
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp)
                    .testTag("privacy_return_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = onAccentColor
                )
            ) {
                Text(
                    text = "Back to Calculator",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Footer Copyright
            Text(
                text = "© 2026 PixelabsDev. All rights reserved.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = subtextColor,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun LegalBadge(text: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDark) Color(0xFF2C2738) else Color(0xFFEDE4F5))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF593F8E)
            )
        )
    }
}

@Composable
fun LegalSectionCard(
    number: String,
    title: String,
    body: String,
    cardBg: Color,
    textColor: Color,
    subtextColor: Color,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = subtextColor,
                    lineHeight = 20.sp,
                    fontSize = 13.5.sp
                ),
                modifier = Modifier.padding(start = 34.dp)
            )
        }
    }
}

@Composable
fun ArrowBackIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.58f, h * 0.22f)
            lineTo(w * 0.28f, h * 0.5f)
            lineTo(w * 0.58f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.28f, h * 0.5f),
            end = Offset(w * 0.78f, h * 0.5f),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun LegalPolicyIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Draw document page
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.1f)
            lineTo(w * 0.65f, h * 0.1f)
            lineTo(w * 0.85f, h * 0.3f)
            lineTo(w * 0.85f, h * 0.9f)
            lineTo(w * 0.2f, h * 0.9f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
        // Page corner fold
        drawLine(
            color = color,
            start = Offset(w * 0.65f, h * 0.1f),
            end = Offset(w * 0.65f, h * 0.3f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(w * 0.65f, h * 0.3f),
            end = Offset(w * 0.85f, h * 0.3f),
            strokeWidth = 2.dp.toPx()
        )
        // Document lines
        drawLine(
            color = color,
            start = Offset(w * 0.32f, h * 0.45f),
            end = Offset(w * 0.72f, h * 0.45f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.32f, h * 0.60f),
            end = Offset(w * 0.72f, h * 0.60f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.32f, h * 0.75f),
            end = Offset(w * 0.55f, h * 0.75f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
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


