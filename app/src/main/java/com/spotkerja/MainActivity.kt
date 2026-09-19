package com.spotkerja

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spotkerja.data.ScanSession
import com.spotkerja.data.WorkMode
import com.spotkerja.export.Exporter
import com.spotkerja.sense.ScanProgress
import com.spotkerja.ui.HistoryScreen
import com.spotkerja.ui.HomeScreen
import com.spotkerja.ui.ResultsScreen
import com.spotkerja.ui.ScanScreen
import com.spotkerja.ui.theme.SpotkerjaTheme
import com.spotkerja.vm.AppViewModel
import com.spotkerja.vm.ScanPhase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpotkerjaTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SpotkerjaApp()
                }
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val RESULTS = "results"
    const val HISTORY = "history"
    const val SESSION = "session/{id}"
    fun session(id: String) = "session/$id"
}

@Composable
fun SpotkerjaApp(vm: AppViewModel = viewModel()) {
    val nav: NavHostController = rememberNavController()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val mode by vm.mode.collectAsState()
    val duration by vm.durationSec.collectAsState()
    val phase by vm.phase.collectAsState()
    val spotLabels by vm.spotLabels.collectAsState()
    val currentSpot by vm.currentSpotIndex.collectAsState()
    val spots by vm.spots.collectAsState()
    val history by vm.history.collectAsState()
    val progress by vm.scanProgress.collectAsState()

    // Pindah ke layar hasil saat fase RESULTS
    LaunchedEffect(phase) {
        if (phase == ScanPhase.RESULTS && nav.currentDestination?.route == Routes.SCAN) {
            nav.navigate(Routes.RESULTS) { popUpTo(Routes.HOME) }
        }
    }

    // Keep screen on saat scanning
    val activity = ctx as? MainActivity
    LaunchedEffect(progress.running) {
        if (progress.running) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* lanjut apapun hasilnya — metrik yang tak tersedia ditandai */ }

    fun requestPermsAnd(action: () -> Unit) {
        permLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
        ))
        action()
    }

    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                mode = mode,
                durationSec = duration,
                spotLabels = spotLabels,
                history = history,
                onModeChange = vm::setMode,
                onDurationChange = vm::setDuration,
                onAddSpot = vm::addSpotLabel,
                onRemoveSpot = vm::removeSpotLabel,
                onStartScan = {
                    requestPermsAnd {
                        vm.startScan()
                        nav.navigate(Routes.SCAN)
                    }
                },
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
                onOpenSession = { s -> nav.navigate(Routes.session(s.id)) },
            )
        }
        composable(Routes.SCAN) {
            val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
            ScanScreen(
                spotLabels = spotLabels,
                currentSpotIndex = currentSpot,
                spotsDone = spots,
                progress = progress,
                hasLightSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT) != null,
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
                onShare = {
                    vm.currentSession?.let { s -> scope.launch { Exporter.share(ctx, s) } }
                },
                onNewScan = {
                    vm.reset()
                    nav.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                sessions = history,
                onOpen = { s -> nav.navigate(Routes.session(s.id)) },
                onDelete = vm::deleteSession,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SESSION) { backStack ->
            val id = backStack.arguments?.getString("id")
            var session by remember { mutableStateOf<ScanSession?>(null) }
            LaunchedEffect(id) { id?.let { vm.sessionById(it) { s -> session = s } } }
            session?.let { s ->
                ResultsScreen(
                    spots = s.spots,
                    mode = WorkMode.entries.firstOrNull { it.label == s.mode } ?: WorkMode.WORK,
                    onShare = { scope.launch { Exporter.share(ctx, s) } },
                    onNewScan = {
                        vm.reset()
                        nav.popBackStack(Routes.HOME, inclusive = false)
                    },
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
