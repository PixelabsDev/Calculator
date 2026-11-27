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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calculator.CalculatorViewModel
import com.example.calculator.DisplayTypographyScaler
import com.example.calculator.ExpressionFormatter
import com.example.calculator.ThousandsOutputTransformation
import com.example.data.HistoryEntry
import com.example.report.ReportIssueDialog
import com.example.ui.theme.MyApplicationTheme

val LocalButtonAspectRatio = compositionLocalOf { 1f }

@Immutable
data class CalculatorColors(
    val appBg: Color,
    val displayBg: Color,
    val keypadBg: Color,
    val numberBtnBg: Color,
    val numberBtnTxt: Color,
    val operatorBtnBg: Color,
    val operatorBtnTxt: Color,
    val scientificBtnBg: Color,
    val scientificBtnTxt: Color,
    val clearBtnBg: Color,
    val clearBtnTxt: Color,
    val equalsBtnBg: Color,
    val equalsBtnTxt: Color,
    val activeToggleBg: Color,
    val activeToggleTxt: Color,
    val icon: Color,
    val displayText: Color,
    val displayPlaceholder: Color,
    val displayPreview: Color,
    val cursor: Color
)

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

            CalculatorAppScreen(viewModel)
        }
    }
}

@Composable
fun CalculatorAppScreen(viewModel: CalculatorViewModel = viewModel()) {
    val context = LocalContext.current
    val input by viewModel.inputState.collectAsState()
    val inputFieldValue by viewModel.inputFieldState.collectAsState()
    val keypadEventCounter by viewModel.keypadEventCounter.collectAsState()
    val cursorDismissSignal by viewModel.cursorDismissSignal.collectAsState()
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

    // Adaptive Material You theming: Android 12+ dynamic wallpaper palette, pre-Android 10 defaults to Dark fallback
    val themeMode by viewModel.themeMode.collectAsState()
    val systemDark = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isSystemInDarkTheme()
    } else {
        true
    }
    val isDark = when (themeMode) {
        com.example.calculator.ThemeMode.SYSTEM -> systemDark
        com.example.calculator.ThemeMode.LIGHT -> false
        com.example.calculator.ThemeMode.DARK -> true
    }

    MyApplicationTheme(darkTheme = isDark) {
        val colorScheme = MaterialTheme.colorScheme
        val calcColors = CalculatorColors(
            appBg = colorScheme.surface,
            displayBg = colorScheme.surfaceContainerLow,
            keypadBg = colorScheme.surface,
            numberBtnBg = colorScheme.surfaceContainerHigh,
            numberBtnTxt = colorScheme.onSurface,
            operatorBtnBg = colorScheme.tertiaryContainer,
            operatorBtnTxt = colorScheme.onTertiaryContainer,
            scientificBtnBg = colorScheme.secondaryContainer,
            scientificBtnTxt = colorScheme.onSecondaryContainer,
            clearBtnBg = colorScheme.secondaryContainer,
            clearBtnTxt = colorScheme.onSecondaryContainer,
            equalsBtnBg = colorScheme.primaryContainer,
            equalsBtnTxt = colorScheme.onPrimaryContainer,
            activeToggleBg = colorScheme.primary,
            activeToggleTxt = colorScheme.onPrimary,
            icon = colorScheme.onSurface,
            displayText = colorScheme.onSurface,
            displayPlaceholder = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            displayPreview = colorScheme.onSurfaceVariant,
            cursor = colorScheme.primary
        )

        val displayBgColor = calcColors.displayBg
        val appBgColor = calcColors.appBg
        val keypadBgColor = calcColors.keypadBg
        val numberBtnBg = calcColors.numberBtnBg
        val numberBtnTxt = calcColors.numberBtnTxt
        val operatorBtnBg = calcColors.operatorBtnBg
        val operatorBtnTxt = calcColors.operatorBtnTxt
        val scientificBtnBg = calcColors.scientificBtnBg
        val scientificBtnTxt = calcColors.scientificBtnTxt
        val equalsBtnBg = calcColors.equalsBtnBg
        val equalsBtnTxt = calcColors.equalsBtnTxt
        val iconColor = calcColors.icon

        val expressionFocusRequester = remember { FocusRequester() }
        LaunchedEffect(showHistoryDrawer, showReportDialog, isPortrait) {
            if (!showHistoryDrawer && !showReportDialog) {
                try {
                    expressionFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }
        }

        BackHandler(enabled = showHistoryDrawer) {
            showHistoryDrawer = false
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = appBgColor,
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
                                        color = colorScheme.onSurfaceVariant,
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
                                                text = ExpressionFormatter.formatForDisplay(item.expression),
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    color = colorScheme.onSurfaceVariant,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    fontFeatureSettings = "tnum"
                                                ),
                                                textAlign = TextAlign.End,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = ExpressionFormatter.formatResultForDisplay(item.result),
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    color = colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = if (item.result.length > 12) 24.sp else 32.sp,
                                                    fontFeatureSettings = "tnum"
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
                                            color = colorScheme.onSurfaceVariant
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

                        val rawPreviewText = if (result.isNotEmpty()) result else if (input.isNotEmpty()) input else "0"
                        val currentPreviewText = remember(rawPreviewText) {
                            ExpressionFormatter.formatResultForDisplay(rawPreviewText)
                        }
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
                            color = colorScheme.surfaceContainerHigh
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
                                        .background(colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
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
                                            color = colorScheme.onSurfaceVariant,
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
                                                color = colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFeatureSettings = "tnum"
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
                        // Material You Google Calculator Display Card (ergonomic center-bottom anchor)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .defaultMinSize(minHeight = 148.dp)
                                .padding(horizontal = 12.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = displayBgColor
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                // Subtle pull-down affordance pill signaling swipe/tap to open History
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showHistoryDrawer = true }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(colorScheme.onSurfaceVariant.copy(alpha = 0.28f))
                                    )
                                }

                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.55f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showHistoryDrawer = true }
                                )

                                CalculatorExpressionDisplay(
                                    fieldValue = inputFieldValue,
                                    keypadEventCounter = keypadEventCounter,
                                    cursorDismissSignal = cursorDismissSignal,
                                    onFieldValueChange = { viewModel.onInputFieldValueChange(it) },
                                    onEdgeTapSnapCursor = { targetIndex -> viewModel.moveCursorTo(targetIndex, dismissHandle = true) },
                                    previewResult = result,
                                    inputSizeTiers = DisplayTypographyScaler.PORTRAIT_INPUT_SIZES_SP,
                                    previewSizeTiers = DisplayTypographyScaler.PORTRAIT_PREVIEW_SIZES_SP,
                                    inputBaseStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Normal),
                                    previewBaseStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Normal),
                                    textColor = calcColors.displayText,
                                    placeholderColor = calcColors.displayPlaceholder,
                                    previewColor = calcColors.displayPreview,
                                    cursorColor = calcColors.cursor,
                                    focusRequester = expressionFocusRequester,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentVerticalArrangement = Arrangement.Center,
                                    spaceBetweenInputAndPreview = 8.dp
                                )

                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.22f)
                                )
                            }
                        }

                        // Minimal Chevron Expand Button & Mode Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 4.dp),
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
                                    color = colorScheme.onSurfaceVariant
                                )
                            }

                            if (isRadMode) {
                                Text(
                                    text = "RAD",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.primary
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
                                        bgColor = if (isRadMode) calcColors.activeToggleBg else scientificBtnBg,
                                        txtColor = if (isRadMode) calcColors.activeToggleTxt else scientificBtnTxt,
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
                                        bgColor = if (isInvMode) calcColors.activeToggleBg else scientificBtnBg,
                                        txtColor = if (isInvMode) calcColors.activeToggleTxt else scientificBtnTxt,
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
                                bgColor = calcColors.clearBtnBg,
                                txtColor = calcColors.clearBtnTxt,
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
                inputFieldValue = inputFieldValue,
                result = result,
                isRadMode = isRadMode,
                isInvMode = isInvMode,
                colors = calcColors,
                focusRequester = expressionFocusRequester
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
}

@Composable
fun InlineHistoryRow(
    item: HistoryEntry,
    onSelect: (String) -> Unit,
    onDelete: (Long) -> Unit,
    isDark: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
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
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${ExpressionFormatter.formatForDisplay(item.expression)}= ${ExpressionFormatter.formatResultForDisplay(item.result)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    fontFeatureSettings = "tnum"
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
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceContainer)
            .clickable { onSelect(item.expression) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = ExpressionFormatter.formatForDisplay(item.expression),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onSurfaceVariant,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "= ${ExpressionFormatter.formatResultForDisplay(item.result)}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    fontFeatureSettings = "tnum"
                )
            )
        }

        IconButton(
            onClick = { onDelete(item.id) }
        ) {
            TrashIcon(
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Shared Google Calculator-style interactive expression display backed by Compose [TextFieldState].
 * Features:
 * - Suppressed system keyboard via [InterceptPlatformTextInput] & null [LocalTextInputService].
 * - Native long-press scrubbing bubble + Copy/Paste context menu.
 * - Instant handle & toolbar collapse when any calculator keypad key or far-edge tap occurs.
 * - Pixel-exact single-line typography auto-scaling via [TextMeasurer] and [DisplayTypographyScaler].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalculatorExpressionDisplay(
    fieldValue: TextFieldValue,
    keypadEventCounter: Int,
    cursorDismissSignal: Int,
    onFieldValueChange: (TextFieldValue) -> Unit,
    onEdgeTapSnapCursor: (Int) -> Unit,
    previewResult: String,
    inputSizeTiers: FloatArray,
    previewSizeTiers: FloatArray,
    inputBaseStyle: TextStyle,
    previewBaseStyle: TextStyle,
    textColor: Color,
    placeholderColor: Color,
    previewColor: Color,
    cursorColor: Color,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    contentVerticalArrangement: Arrangement.Vertical = Arrangement.Bottom,
    spaceBetweenInputAndPreview: Dp = 8.dp
) {
    val textFieldState = remember {
        TextFieldState(
            initialText = fieldValue.text,
            initialSelection = fieldValue.selection
        )
    }
    val outputTransformation = remember { ThousandsOutputTransformation() }
    val inputScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val textToolbar = LocalTextToolbar.current
    val viewConfig = LocalViewConfiguration.current

    var lastTextLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isHandleLikelyVisible by remember { mutableStateOf(false) }
    var suppressSyncFromField by remember { mutableStateOf(false) }
    val latestFieldValue by rememberUpdatedState(fieldValue)

    // Sync programmatic ViewModel edits into Compose TextFieldState cleanly
    LaunchedEffect(fieldValue) {
        val currentText = textFieldState.text.toString()
        val currentSel = textFieldState.selection
        if (currentText != fieldValue.text || currentSel != fieldValue.selection) {
            val textChanged = currentText != fieldValue.text
            suppressSyncFromField = true
            try {
                textFieldState.edit {
                    if (textChanged) {
                        replace(0, length, fieldValue.text)
                    }
                    selection = fieldValue.selection
                }
            } finally {
                suppressSyncFromField = false
            }
            if (textChanged) {
                isHandleLikelyVisible = false
            }
        }
    }

    // Propagate interactive taps, drags, and clipboard pastes back to CalculatorViewModel
    LaunchedEffect(textFieldState) {
        snapshotFlow {
            TextFieldValue(
                text = textFieldState.text.toString(),
                selection = textFieldState.selection
            )
        }
            .distinctUntilChanged()
            .collect { updated ->
                if (!suppressSyncFromField && updated != latestFieldValue) {
                    onFieldValueChange(updated)
                }
            }
    }

    // Dismiss floating cursor bubble & Copy/Paste toolbar immediately on any keypad key or edge tap
    var lastKeypadCount by remember { mutableIntStateOf(keypadEventCounter) }
    var lastDismissSignal by remember { mutableIntStateOf(cursorDismissSignal) }

    LaunchedEffect(keypadEventCounter, cursorDismissSignal) {
        val keypadPressed = keypadEventCounter != lastKeypadCount
        val edgeDismissRequested = cursorDismissSignal != lastDismissSignal
        lastKeypadCount = keypadEventCounter
        lastDismissSignal = cursorDismissSignal

        if (!keypadPressed && !edgeDismissRequested) return@LaunchedEffect

        if (textToolbar.status == TextToolbarStatus.Shown) {
            textToolbar.hide()
        }

        if (isHandleLikelyVisible || edgeDismissRequested) {
            isHandleLikelyVisible = false
            focusManager.clearFocus(force = true)
            withFrameNanos { }
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    val formattedInput = remember(fieldValue.text) {
        if (fieldValue.text.isEmpty()) "0" else ExpressionFormatter.formatForDisplay(fieldValue.text)
    }
    val formattedPreview = remember(previewResult) {
        ExpressionFormatter.formatResultForDisplay(previewResult)
    }

    BoxWithConstraints(modifier = modifier) {
        val availableWidthPx = with(density) { (maxWidth - 8.dp).toPx() }.coerceAtLeast(1f)

        val fittedInputFontSp = remember(formattedInput, availableWidthPx, inputBaseStyle, inputSizeTiers) {
            DisplayTypographyScaler.selectFittingFontSizeSp(
                tiersSp = inputSizeTiers,
                maxWidthPx = availableWidthPx
            ) { candidateSp ->
                textMeasurer.measure(
                    text = AnnotatedString(formattedInput),
                    style = inputBaseStyle.copy(
                        fontSize = candidateSp.sp,
                        lineHeight = (candidateSp * 1.15f).sp,
                        fontFamily = FontFamily.SansSerif,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    softWrap = false
                ).size.width.toFloat()
            }
        }

        val fittedPreviewFontSp = remember(formattedPreview, availableWidthPx, previewBaseStyle, previewSizeTiers) {
            if (formattedPreview.isEmpty()) {
                previewSizeTiers.firstOrNull() ?: 32f
            } else {
                DisplayTypographyScaler.selectFittingFontSizeSp(
                    tiersSp = previewSizeTiers,
                    maxWidthPx = availableWidthPx
                ) { candidateSp ->
                    textMeasurer.measure(
                        text = AnnotatedString(formattedPreview),
                        style = previewBaseStyle.copy(
                            fontSize = candidateSp.sp,
                            lineHeight = (candidateSp * 1.2f).sp,
                            fontFamily = FontFamily.SansSerif,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1,
                        softWrap = false
                    ).size.width.toFloat()
                }
            }
        }

        val dynamicInputStyle = remember(fittedInputFontSp, inputBaseStyle, textColor) {
            inputBaseStyle.copy(
                fontSize = fittedInputFontSp.sp,
                lineHeight = (fittedInputFontSp * 1.15f).sp,
                color = textColor,
                textAlign = TextAlign.End,
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "tnum"
            )
        }

        val dynamicPreviewStyle = remember(fittedPreviewFontSp, previewBaseStyle, previewColor) {
            previewBaseStyle.copy(
                fontSize = fittedPreviewFontSp.sp,
                lineHeight = (fittedPreviewFontSp * 1.2f).sp,
                color = previewColor,
                textAlign = TextAlign.End,
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "tnum"
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = contentVerticalArrangement,
            horizontalAlignment = Alignment.End
        ) {
            val edgeAndScrubObserver = Modifier.pointerInput(viewConfig, density) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent(PointerEventPass.Final)
                        val down = downEvent.changes.firstOrNull { it.pressed } ?: continue
                        val downTime = down.uptimeMillis
                        val downPos = down.position
                        var maxDistSq = 0f
                        var lastPos = downPos
                        var upTime = downTime

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            lastPos = change.position
                            val dx = lastPos.x - downPos.x
                            val dy = lastPos.y - downPos.y
                            val distSq = dx * dx + dy * dy
                            if (distSq > maxDistSq) maxDistSq = distSq
                            upTime = change.uptimeMillis
                            if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                                break
                            }
                        }

                        val elapsed = upTime - downTime
                        val slopSq = viewConfig.touchSlop * viewConfig.touchSlop
                        val isQuickTap = elapsed < viewConfig.longPressTimeoutMillis && maxDistSq <= slopSq

                        if (!isQuickTap) {
                            // User long-pressed or scrubbed: keep native teardrop bubble & toolbar active
                            isHandleLikelyVisible = true
                        } else {
                            val edgeZonePx = with(density) { 20.dp.toPx() }
                            val scrollOffset = inputScrollState.value.toFloat()
                            val layout = lastTextLayout
                            val visibleLeft = layout?.getLineLeft(0)?.minus(scrollOffset)
                            val visibleRight = layout?.getLineRight(0)?.minus(scrollOffset)
                            val edgeTarget = DisplayTypographyScaler.resolveEdgeTapCursorIndex(
                                tapX = lastPos.x,
                                fieldWidthPx = size.width.toFloat(),
                                edgeZonePx = edgeZonePx,
                                lineLeftPx = visibleLeft,
                                lineRightPx = visibleRight,
                                textLength = textFieldState.text.length
                            )
                            if (edgeTarget != null) {
                                onEdgeTapSnapCursor(edgeTarget)
                            } else {
                                isHandleLikelyVisible = true
                            }
                        }
                    }
                }
            }

            InterceptPlatformTextInput(
                interceptor = { _, _ ->
                    awaitCancellation()
                }
            ) {
                @Suppress("DEPRECATION")
                CompositionLocalProvider(LocalTextInputService provides null) {
                    BasicTextField(
                        state = textFieldState,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        scrollState = inputScrollState,
                        textStyle = dynamicInputStyle,
                        cursorBrush = SolidColor(cursorColor),
                        outputTransformation = outputTransformation,
                        onTextLayout = { getResult ->
                            lastTextLayout = getResult()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(edgeAndScrubObserver)
                            .focusRequester(focusRequester)
                            .then(
                                if (fieldValue.text.isNotEmpty()) {
                                    Modifier.testTag("calculation_input")
                                } else {
                                    Modifier
                                }
                            ),
                        decorator = TextFieldDecorator { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (fieldValue.text.isEmpty()) {
                                    Text(
                                        text = "0",
                                        style = dynamicInputStyle.copy(color = placeholderColor),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier
                                            .padding(end = 2.dp)
                                            .testTag("calculation_input")
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            val previewScrollState = rememberScrollState()
            LaunchedEffect(formattedPreview) {
                if (formattedPreview.isNotEmpty()) {
                    previewScrollState.animateScrollTo(previewScrollState.maxValue)
                }
            }

            AnimatedContent(
                targetState = formattedPreview,
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
                    Column {
                        Spacer(modifier = Modifier.height(spaceBetweenInputAndPreview))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(previewScrollState),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Text(
                                text = previewText,
                                style = dynamicPreviewStyle,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.testTag("calculation_preview")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandscapeCalculatorLayout(
    viewModel: CalculatorViewModel,
    inputFieldValue: TextFieldValue,
    result: String,
    isRadMode: Boolean,
    isInvMode: Boolean,
    colors: CalculatorColors,
    focusRequester: FocusRequester
) {
    val colorScheme = MaterialTheme.colorScheme
    val vibrate = rememberVibrator()
    val keypadEventCounter by viewModel.keypadEventCounter.collectAsState()
    val cursorDismissSignal by viewModel.cursorDismissSignal.collectAsState()
    val displayBgColor = colors.displayBg
    val keypadBgColor = colors.keypadBg
    val numberBtnBg = colors.numberBtnBg
    val numberBtnTxt = colors.numberBtnTxt
    val operatorBtnBg = colors.operatorBtnBg
    val operatorBtnTxt = colors.operatorBtnTxt
    val scientificBtnBg = colors.scientificBtnBg
    val scientificBtnTxt = colors.scientificBtnTxt
    val equalsBtnBg = colors.equalsBtnBg
    val equalsBtnTxt = colors.equalsBtnTxt
    val activeSciBg = colors.activeToggleBg
    val activeSciTxt = colors.activeToggleTxt
    val alertBg = colors.clearBtnBg
    val alertTxt = colors.clearBtnTxt

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
        // Top ~28% Screen Height: Dedicated Slim Horizontal Display Panel with guaranteed headroom
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.28f),
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
                            colorScheme.secondaryContainer
                        } else {
                            colorScheme.surfaceContainerHigh
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
                                color = if (isLandScientificExpanded) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isLandScientificExpanded) "Scientific" else "Basic",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLandScientificExpanded) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    if (isRadMode) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "RAD",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }

                // Right Side: Interactive Cursor Expression & Live Preview
                CalculatorExpressionDisplay(
                    fieldValue = inputFieldValue,
                    keypadEventCounter = keypadEventCounter,
                    cursorDismissSignal = cursorDismissSignal,
                    onFieldValueChange = { viewModel.onInputFieldValueChange(it) },
                    onEdgeTapSnapCursor = { targetIndex -> viewModel.moveCursorTo(targetIndex, dismissHandle = true) },
                    previewResult = result,
                    inputSizeTiers = DisplayTypographyScaler.LANDSCAPE_INPUT_SIZES_SP,
                    previewSizeTiers = DisplayTypographyScaler.LANDSCAPE_PREVIEW_SIZES_SP,
                    inputBaseStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Normal),
                    previewBaseStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                    textColor = colors.displayText,
                    placeholderColor = colors.displayPlaceholder,
                    previewColor = colors.displayPreview,
                    cursorColor = colors.cursor,
                    focusRequester = focusRequester,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 12.dp, end = 4.dp),
                    contentVerticalArrangement = Arrangement.Center,
                    spaceBetweenInputAndPreview = 2.dp
                )
            }
        }

        // Bottom ~72% Screen Height: Keypad Container (4-Row Google Calculator Landscape Grid)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.72f),
            shape = RoundedCornerShape(16.dp),
            color = keypadBgColor
        ) {
            CompositionLocalProvider(LocalButtonAspectRatio provides -1f) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val btnShape = CircleShape
                    val sciFontSize = 13.sp
                    val numFontSize = 18.sp

                    // Left Side: 3 Columns Scientific Functions (Collapsible, 4 rows)
                    if (sciWeight > 0.05f) {
                        Column(
                            modifier = Modifier
                                .weight(sciWeight)
                                .fillMaxHeight()
                                .clipToBounds()
                                .graphicsLayer {
                                    alpha = (sciWeight / 3f).coerceIn(0f, 1f)
                                },
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: [√] [Deg/Rad] [Inv]
                            Row(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KeypadButton(
                                    label = "√",
                                    bgColor = scientificBtnBg,
                                    txtColor = scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.handleKeyPress("√") }

                                KeypadButton(
                                    label = if (isRadMode) "Rad" else "Deg",
                                    bgColor = if (isRadMode) activeSciBg else scientificBtnBg,
                                    txtColor = if (isRadMode) activeSciTxt else scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    fontWeight = FontWeight.Bold,
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.toggleRadMode() }

                                KeypadButton(
                                    label = "Inv",
                                    bgColor = if (isInvMode) activeSciBg else scientificBtnBg,
                                    txtColor = if (isInvMode) activeSciTxt else scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    fontWeight = FontWeight.Bold,
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.toggleInvMode() }
                            }

                            // Row 2: [π] [sin/sin⁻¹] [e]
                            Row(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KeypadButton(
                                    label = "π",
                                    bgColor = scientificBtnBg,
                                    txtColor = scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.handleKeyPress("π") }

                                KeypadButton(
                                    label = if (isInvMode) "sin⁻¹" else "sin",
                                    bgColor = scientificBtnBg,
                                    txtColor = scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = btnShape,
                                    fontSize = 12.sp
                                ) { viewModel.handleKeyPress("sin") }

                                KeypadButton(
                                    label = "e",
                                    bgColor = scientificBtnBg,
                                    txtColor = scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.handleKeyPress("e") }
                            }

                            // Row 3: [^] [cos/cos⁻¹] [ln]
                            Row(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KeypadButton(
                                    label = "^",
                                    bgColor = scientificBtnBg,
                                    txtColor = scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.handleKeyPress("^") }

                                KeypadButton(
                                    label = if (isInvMode) "cos⁻¹" else "cos",
                                    bgColor = scientificBtnBg,
                                    txtColor = scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = btnShape,
                                    fontSize = 12.sp
                                ) { viewModel.handleKeyPress("cos") }

                                KeypadButton(
                                    label = "ln",
                                    bgColor = scientificBtnBg,
                                    txtColor = scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.handleKeyPress("ln") }
                            }

                            // Row 4: [!] [tan/tan⁻¹] [log]
                            Row(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KeypadButton(
                                    label = "!",
                                    bgColor = scientificBtnBg,
                                    txtColor = scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.handleKeyPress("!") }

                                KeypadButton(
                                    label = if (isInvMode) "tan⁻¹" else "tan",
                                    bgColor = scientificBtnBg,
                                    txtColor = scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = btnShape,
                                    fontSize = 12.sp
                                ) { viewModel.handleKeyPress("tan") }

                                KeypadButton(
                                    label = "log",
                                    bgColor = scientificBtnBg,
                                    txtColor = scientificBtnTxt,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = btnShape,
                                    fontSize = sciFontSize
                                ) { viewModel.handleKeyPress("log") }
                            }
                        }
                    }

                    // Right Side: 5 Columns Primary Numeric & Operator Keypad (4 rows)
                    Column(
                        modifier = Modifier
                            .weight(5f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: [7] [8] [9] [AC] [×]
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeypadButton(
                                label = "7",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("7") }

                            KeypadButton(
                                label = "8",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("8") }

                            KeypadButton(
                                label = "9",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("9") }

                            KeypadButton(
                                label = "AC",
                                bgColor = alertBg,
                                txtColor = alertTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Bold,
                                shape = btnShape,
                                fontSize = 15.sp
                            ) { viewModel.handleKeyPress("AC") }

                            KeypadButton(
                                label = "×",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("×") }
                        }

                        // Row 2: [4] [5] [6] [( )] [-]
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeypadButton(
                                label = "4",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("4") }

                            KeypadButton(
                                label = "5",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("5") }

                            KeypadButton(
                                label = "6",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("6") }

                            KeypadButton(
                                label = "( )",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = 14.sp
                            ) { viewModel.handleKeyPress("( )") }

                            KeypadButton(
                                label = "-",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("-") }
                        }

                        // Row 3: [1] [2] [3] [%] [+]
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeypadButton(
                                label = "1",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("1") }

                            KeypadButton(
                                label = "2",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("2") }

                            KeypadButton(
                                label = "3",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("3") }

                            KeypadButton(
                                label = "%",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = btnShape,
                                fontSize = 16.sp
                            ) { viewModel.handleKeyPress("%") }

                            KeypadButton(
                                label = "+",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("+") }
                        }

                        // Row 4: [0] [.] [⌫] [÷] [=]
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeypadButton(
                                label = "0",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Medium,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("0") }

                            KeypadButton(
                                label = ".",
                                bgColor = numberBtnBg,
                                txtColor = numberBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Bold,
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress(".") }

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
                                    .semantics { contentDescription = "Backspace" }
                                    .testTag("btn_backspace"),
                                contentAlignment = Alignment.Center
                            ) {
                                BackspaceIcon(color = operatorBtnTxt)
                            }

                            KeypadButton(
                                label = "÷",
                                bgColor = operatorBtnBg,
                                txtColor = operatorBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = btnShape,
                                fontSize = numFontSize
                            ) { viewModel.handleKeyPress("÷") }

                            KeypadButton(
                                label = "=",
                                bgColor = equalsBtnBg,
                                txtColor = equalsBtnTxt,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                fontWeight = FontWeight.Bold,
                                shape = btnShape,
                                fontSize = 22.sp
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
    val colorScheme = MaterialTheme.colorScheme
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
                            if (isScreenshotBlocked) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isScreenshotBlocked) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isScreenshotBlocked) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
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


