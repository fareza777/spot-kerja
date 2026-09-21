package com.spotkerja

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.spotkerja.ads.Ads
import com.spotkerja.ads.BannerAd
import com.spotkerja.data.ScanSession
import com.spotkerja.data.WorkMode
import com.spotkerja.export.ExportFormat
import com.spotkerja.export.Exporter
import com.spotkerja.export.InfoColors
import com.spotkerja.settings.ThemeMode
import com.spotkerja.ui.FocusScreen
import com.spotkerja.ui.HistoryScreen
import com.spotkerja.ui.HomeScreen
import com.spotkerja.ui.OnboardingOverlay
import com.spotkerja.ui.PostureScreen
import com.spotkerja.ui.ResultsScreen
import com.spotkerja.ui.ScanScreen
import com.spotkerja.ui.SettingsScreen
import com.spotkerja.ui.theme.LocalPalette
import com.spotkerja.ui.theme.LocalScoreColor
import com.spotkerja.ui.theme.SpotkerjaTheme
import com.spotkerja.vm.AppViewModel
import com.spotkerja.vm.ScanPhase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object { const val ACTION_FAST_SCAN = "com.spotwise.app.FAST_SCAN" }

    /** Ditandai saat widget Fast Scan menap; dikonsumsi sekali oleh UI. */
    private val widgetFastScan = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this)
        Ads.preloadInterstitial(this)
        widgetFastScan.value = intent?.action == ACTION_FAST_SCAN
        setContent {
            val vm: AppViewModel = viewModel()
            val theme by vm.theme.collectAsState()
            val themeMode by vm.themeMode.collectAsState()

            // Status/nav bar icon mengikuti tema terpilih — fix manual LIGHT
            // mode di sistem gelap (enableEdgeToEdge hanya baca tema sistem).
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val view = LocalView.current
            LaunchedEffect(dark) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            SpotkerjaTheme(theme, themeMode) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SpotkerjaApp(vm, widgetFastScan)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_FAST_SCAN) widgetFastScan.value = true
    }
}

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val RESULTS = "results"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val SESSION = "session/{id}"
    const val FOCUS = "focus/{label}"
    const val POSTURE = "posture"
    fun session(id: String) = "session/$id"
    fun focus(label: String) = "focus/${android.net.Uri.encode(label)}"
}

private data class Tab(val route: String, val label: String,
                       val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Scan", Icons.Default.MyLocation),
    Tab(Routes.HISTORY, "History", Icons.Default.History),
    Tab(Routes.SETTINGS, "Settings", Icons.Default.Settings),
)

@Composable
fun SpotkerjaApp(vm: AppViewModel, widgetFastScan: MutableState<Boolean>) {
    val nav: NavHostController = rememberNavController()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val p = LocalPalette.current

    val mode by vm.mode.collectAsState()
    val duration by vm.durationSec.collectAsState()
    val phase by vm.phase.collectAsState()
    val spotCount by vm.spotCount.collectAsState()
    val spotNames by vm.spotNames.collectAsState()
    val currentSpot by vm.currentSpotIndex.collectAsState()
    val spots by vm.spots.collectAsState()
    val history by vm.history.collectAsState()
    val progress by vm.scanProgress.collectAsState()
    val adsEnabled by vm.adsEnabled.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val scanOptions by vm.scanOptions.collectAsState()
    val fastDuration by vm.fastDurationSec.collectAsState()
    val isFastScan by vm.isFastScan.collectAsState()
    val onboarded by vm.onboarded.collectAsState()
    val presets by vm.presets.collectAsState()
    val blindTest by vm.blindTest.collectAsState()
    val focusMinutes by vm.focusMinutes.collectAsState()
    val currentSession by vm.currentSession.collectAsState()
    var exporting by remember { mutableStateOf(false) }

    val scoreColor = LocalScoreColor.current
    val infoColors = remember(p) {
        InfoColors(
            bg = p.bg.toArgb(), card = p.card.toArgb(), border = p.border.toArgb(),
            accent = p.accent.toArgb(), accent2 = p.accent2.toArgb(),
            gold = p.gold.toArgb(), text = p.text.toArgb(), dim = p.textDim.toArgb(),
            good = scoreColor(80f).toArgb(), mid = scoreColor(60f).toArgb(),
            bad = scoreColor(10f).toArgb(),
        )
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBottomBar = route in TABS.map { it.route } && onboarded

    LaunchedEffect(phase) {
        if (phase == ScanPhase.RESULTS && route == Routes.SCAN) {
            nav.navigate(Routes.RESULTS) { popUpTo(Routes.HOME) }
            if (adsEnabled) {
                (ctx as? MainActivity)?.let { Ads.maybeShowInterstitial(it) }
            }
        }
    }

    val activity = ctx as? MainActivity
    LaunchedEffect(progress.running) {
        if (progress.running) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // Izin mic/lokasi harus selesai SEBELUM scan mulai — kalau tidak, sampler
    // noise/orientasi kehilangan sampel awal saat dialog izin tampil.
    var pendingScan by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        pendingScan?.invoke()
        pendingScan = null
    }
    fun scanWithPerms(action: () -> Unit) {
        pendingScan = action
        permLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.CAMERA,
        ))
    }

    // Fast Scan dari home-screen widget — minta izin dulu lalu jalan.
    LaunchedEffect(widgetFastScan.value) {
        if (widgetFastScan.value) {
            widgetFastScan.value = false
            scanWithPerms {
                vm.fastScanFromWidget()
                nav.navigate(Routes.SCAN) { launchSingleTop = true }
            }
        }
    }

    fun export(format: ExportFormat) {
        val s = vm.currentSession.value ?: return
        exporting = true
        scope.launch {
            runCatching { Exporter.export(ctx, s, format, infoColors) }
            exporting = false
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FloatingNavBar(route) { r ->
                    nav.navigate(r) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            p.accent.copy(alpha = 0.07f),
                            p.bg.copy(alpha = 0f),
                        ),
                        startY = 0f,
                        endY = 600f,
                    )
                )
                .background(p.bg)
        ) {
        NavHost(
            nav, startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    mode = mode,
                    durationSec = duration,
                    spotCount = spotCount,
                    spotNames = spotNames,
                    history = history,
                    onModeChange = vm::setMode,
                    onDurationChange = vm::setDuration,
                    onSpotCountChange = vm::setSpotCount,
                    onSpotNameChange = vm::setSpotName,
                    onStartScan = {
                        scanWithPerms {
                            vm.startScan()
                            nav.navigate(Routes.SCAN)
                        }
                    },
                    onFastScan = {
                        scanWithPerms {
                            vm.startFastScan()
                            nav.navigate(Routes.SCAN)
                        }
                    },
                    fastDurationSec = fastDuration,
                    onOpenSession = { s -> nav.navigate(Routes.session(s.id)) },
                    presets = presets,
                    onApplyPreset = vm::applyPreset,
                    blindTest = blindTest,
                    onBlindChange = vm::setBlindTest,
                    bannerAd = if (adsEnabled) ({
                        BannerAd(Modifier
                            .fillMaxWidth()
                            .wrapContentHeight())
                    }) else null,
                )
            }
            composable(Routes.SCAN) {
                val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
                ScanScreen(
                    spotNames = if (isFastScan) listOf("My Spot") else spotNames,
                    currentSpotIndex = currentSpot,
                    spotsDone = spots,
                    progress = progress,
                    hasLightSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT) != null,
                    opts = scanOptions,
                    onStopEarly = vm::stopCurrentSpotEarly,
                    onScanNext = vm::scanNextSpot,
                    onRescan = vm::rescanCurrentSpot,
                    onCancel = {
                        vm.reset()
                        nav.popBackStack(Routes.HOME, inclusive = false)
                    },
                )
            }
            composable(Routes.RESULTS) {
                ResultsScreen(
                    spots = spots,
                    mode = mode,
                    onExport = ::export,
                    onNewScan = {
                        vm.reset()
                        nav.popBackStack(Routes.HOME, inclusive = false)
                    },
                    exporting = exporting,
                    session = currentSession,
                    onFocusSpot = { label -> nav.navigate(Routes.focus(label)) },
                    onPosture = {
                        pendingScan = { nav.navigate(Routes.POSTURE) }
                        permLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    },
                    onCellAssign = { l, x, y ->
                        currentSession?.let { vm.setSpotCell(it.id, l, x, y) } },
                    onPickFavorite = { l ->
                        currentSession?.let { vm.setUserPick(it.id, l) } },
                    onAssignRoom = { r ->
                        currentSession?.let { vm.setSessionRoom(it.id, r) } },
                    bannerAd = if (adsEnabled) ({
                        BannerAd(Modifier
                            .fillMaxWidth()
                            .wrapContentHeight())
                    }) else null,
                )
            }
            composable(Routes.FOCUS) { backStackEntry ->
                val label = backStackEntry.arguments?.getString("label") ?: ""
                FocusScreen(
                    spotLabel = label,
                    onLogMinutes = vm::logFocus,
                    onDone = { nav.popBackStack() },
                )
            }
            composable(Routes.POSTURE) {
                PostureScreen(onDone = { nav.popBackStack() })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    sessions = history,
                    onOpen = { s -> nav.navigate(Routes.session(s.id)) },
                    onDelete = vm::deleteSession,
                    focusMinutes = focusMinutes,
                    bannerAd = if (adsEnabled) ({
                        BannerAd(Modifier
                            .fillMaxWidth()
                            .wrapContentHeight())
                    }) else null,
                )
            }
            composable(Routes.SETTINGS) {
                val theme by vm.theme.collectAsState()
                SettingsScreen(
                    theme = theme,
                    themeMode = themeMode,
                    adsEnabled = adsEnabled,
                    scanOptions = scanOptions,
                    fastDurationSec = fastDuration,
                    onThemeChange = vm::setTheme,
                    onThemeModeChange = vm::setThemeMode,
                    onAdsChange = vm::setAdsEnabled,
                    onScanOptionsChange = vm::setScanOptions,
                    onFastDurationChange = vm::setFastDuration,
                    appVersion = BuildConfig.VERSION_NAME,
                    presets = presets,
                    durationSec = duration,
                    spotCount = spotCount,
                    onSavePreset = vm::saveCurrentAsPreset,
                    onApplyPreset = vm::applyPreset,
                    onDeletePreset = vm::deletePreset,
                )
            }
                composable(Routes.SESSION) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                var session by remember { mutableStateOf<ScanSession?>(null) }
                // Reload saat history berubah (assign room / blind pick / peta).
                LaunchedEffect(id, history) {
                    id?.let { vm.sessionById(it) { s -> session = s } }
                }
                session?.let { s ->
                    ResultsScreen(
                        spots = s.spots,
                        mode = WorkMode.entries.firstOrNull { it.label == s.mode } ?: WorkMode.WORK,
                        onExport = { f ->
                            scope.launch { Exporter.export(ctx, s, f, infoColors) }
                        },
                        onNewScan = {
                            vm.reset()
                            nav.popBackStack(Routes.HOME, inclusive = false)
                        },
                        onBack = { nav.popBackStack() },
                        session = s,
                        onFocusSpot = { l -> nav.navigate(Routes.focus(l)) },
                        onPosture = {
                            pendingScan = { nav.navigate(Routes.POSTURE) }
                            permLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        },
                        onCellAssign = { l, x, y -> vm.setSpotCell(s.id, l, x, y) },
                        onPickFavorite = { l -> vm.setUserPick(s.id, l) },
                        onAssignRoom = { r -> vm.setSessionRoom(s.id, r) },
                    )
                }
            }
        }
        if (!onboarded) {
            OnboardingOverlay(onDone = vm::markOnboarded)
        }
        }
    }
}

/** Floating bottom bar — a soft accent pill slides under the active tab. */
@Composable
private fun FloatingNavBar(current: String?, onSelect: (String) -> Unit) {
    val p = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    Surface(
        Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(26.dp),
        color = p.card,
        border = BorderStroke(1.dp, p.border),
        tonalElevation = 4.dp,
        shadowElevation = 14.dp,
    ) {
        BoxWithConstraints(Modifier.padding(6.dp)) {
            val itemW = maxWidth / TABS.size
            val selIdx = TABS.indexOfFirst { it.route == current }.coerceAtLeast(0)
            val pillX by animateDpAsState(
                targetValue = itemW * selIdx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow),
                label = "navPill",
            )
            Box(
                Modifier.offset(x = pillX)
                    .width(itemW).height(60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(p.accent.copy(alpha = 0.16f))
            )
            Row(Modifier.fillMaxWidth().height(60.dp)) {
                TABS.forEach { tab ->
                    val sel = current == tab.route
                    val tint by animateColorAsState(
                        if (sel) p.accent else p.textDim, label = "tabTint")
                    val iconScale by animateFloatAsState(
                        if (sel) 1.1f else 1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tabScale")
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tab.route)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(tab.icon, tab.label,
                                Modifier.size(22.dp).scale(iconScale), tint = tint)
                            Text(tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = tint,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}
