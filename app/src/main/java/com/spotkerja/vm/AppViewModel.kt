package com.spotkerja.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spotkerja.data.ScanSession
import com.spotkerja.data.ScoreEngine
import com.spotkerja.data.SessionStore
import com.spotkerja.data.SpotResult
import com.spotkerja.data.WorkMode
import com.spotkerja.sense.ScanEngine
import com.spotkerja.sense.ScanProgress
import com.spotkerja.settings.ScanOptions
import com.spotkerja.settings.ScanPreset
import com.spotkerja.settings.SettingsStore
import com.spotkerja.settings.ThemeMode
import com.spotkerja.ui.theme.ThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class ScanPhase { SETUP, SCANNING, RESULTS }

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SessionStore(app)
    private val settings = SettingsStore(app)
    val engine = ScanEngine(app)

    private val _theme = MutableStateFlow(settings.theme)
    val theme: StateFlow<ThemeOption> = _theme

    private val _themeMode = MutableStateFlow(settings.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _mode = MutableStateFlow(WorkMode.WORK)
    val mode: StateFlow<WorkMode> = _mode

    private val _durationSec = MutableStateFlow(settings.durationSec)
    val durationSec: StateFlow<Int> = _durationSec

    private val _fastDurationSec = MutableStateFlow(settings.fastDurationSec)
    val fastDurationSec: StateFlow<Int> = _fastDurationSec

    private val _scanOptions = MutableStateFlow(settings.scanOptions)
    val scanOptions: StateFlow<ScanOptions> = _scanOptions

    /** True saat sesi aktif adalah Fast Scan (1 spot, durasi singkat). */
    private val _isFastScan = MutableStateFlow(false)
    val isFastScan: StateFlow<Boolean> = _isFastScan

    private val _phase = MutableStateFlow(ScanPhase.SETUP)
    val phase: StateFlow<ScanPhase> = _phase

    private val _spotCount = MutableStateFlow(settings.spotCount)
    val spotCount: StateFlow<Int> = _spotCount

    /** Nama spot per index — custom atau default "Spot X". */
    private val _spotNames = MutableStateFlow(
        (0 until settings.spotCount).map { settings.spotName(it) })
    val spotNames: StateFlow<List<String>> = _spotNames

    private val _currentSpotIndex = MutableStateFlow(0)
    val currentSpotIndex: StateFlow<Int> = _currentSpotIndex

    private val _spots = MutableStateFlow<List<SpotResult>>(emptyList())
    val spots: StateFlow<List<SpotResult>> = _spots

    private val _history = MutableStateFlow<List<ScanSession>>(emptyList())
    val history: StateFlow<List<ScanSession>> = _history

    private val _adsEnabled = MutableStateFlow(settings.adsEnabled)
    val adsEnabled: StateFlow<Boolean> = _adsEnabled

    private val _onboarded = MutableStateFlow(settings.onboarded)
    val onboarded: StateFlow<Boolean> = _onboarded

    fun markOnboarded() { _onboarded.value = true; settings.onboarded = true }

    private val _presets = MutableStateFlow(settings.presets())
    val presets: StateFlow<List<ScanPreset>> = _presets

    /** Simpan konfigurasi scan saat ini sebagai preset bernama. */
    fun saveCurrentAsPreset(name: String) {
        settings.savePreset(ScanPreset(
            name = name,
            durationSec = _durationSec.value,
            spotCount = _spotCount.value,
            options = _scanOptions.value,
        ))
        _presets.value = settings.presets()
    }

    fun deletePreset(name: String) {
        settings.deletePreset(name)
        _presets.value = settings.presets()
    }

    /** Terapkan preset — menimpa opsi scan, durasi, dan jumlah spot. */
    fun applyPreset(preset: ScanPreset) {
        setScanOptions(preset.options)
        setDuration(preset.durationSec)
        setSpotCount(preset.spotCount)
    }

    val scanProgress: StateFlow<ScanProgress> = engine.progress

    init { refreshHistory() }

    fun setTheme(t: ThemeOption) { _theme.value = t; settings.theme = t }
    fun setThemeMode(m: ThemeMode) { _themeMode.value = m; settings.themeMode = m }
    fun setMode(m: WorkMode) { _mode.value = m }

    fun setFastDuration(sec: Int) {
        _fastDurationSec.value = sec.coerceIn(10, 30)
        settings.fastDurationSec = _fastDurationSec.value
    }

    fun setScanOptions(o: ScanOptions) { _scanOptions.value = o; settings.scanOptions = o }
    fun setDuration(sec: Int) {
        _durationSec.value = sec.coerceIn(15, 60)
        settings.durationSec = _durationSec.value
    }
    fun setAdsEnabled(v: Boolean) { _adsEnabled.value = v; settings.adsEnabled = v }

    fun setSpotCount(n: Int) {
        val c = n.coerceIn(2, 8)
        _spotCount.value = c
        settings.spotCount = c
        _spotNames.value = (0 until c).map { settings.spotName(it) }
    }

    fun setSpotName(index: Int, name: String) {
        settings.setSpotName(index, name)
        _spotNames.value = (0 until _spotCount.value).map { settings.spotName(it) }
    }

    fun startScan() {
        _isFastScan.value = false
        _spots.value = emptyList()
        _currentSpotIndex.value = 0
        _phase.value = ScanPhase.SCANNING
        startCurrentSpot()
    }

    /** Deep-link dari widget: Fast Scan langsung jalan. */
    fun fastScanFromWidget() {
        if (_phase.value == ScanPhase.SCANNING) return
        startFastScan()
    }

    /** Fast Scan: satu titik saja, durasi singkat, nama "My Spot". */
    fun startFastScan() {
        _isFastScan.value = true
        _spots.value = emptyList()
        _currentSpotIndex.value = 0
        _phase.value = ScanPhase.SCANNING
        startCurrentSpot()
    }

    private fun activeSpotNames(): List<String> =
        if (_isFastScan.value) listOf("My Spot") else _spotNames.value

    private fun activeDuration(): Int =
        if (_isFastScan.value) _fastDurationSec.value else _durationSec.value

    private fun startCurrentSpot() {
        val label = activeSpotNames().getOrNull(_currentSpotIndex.value) ?: return
        engine.start(viewModelScope, label, activeDuration(), _mode.value,
            _scanOptions.value) { result ->
            onSpotDone(result)
        }
    }

    fun stopCurrentSpotEarly() {
        val label = activeSpotNames().getOrNull(_currentSpotIndex.value) ?: return
        engine.stop(label, activeDuration(), _mode.value, _scanOptions.value) { result -> onSpotDone(result) }
    }

    private fun onSpotDone(result: SpotResult) {
        _spots.value = _spots.value + result
        val next = _currentSpotIndex.value + 1
        _currentSpotIndex.value = next
        if (next >= activeSpotNames().size) finishSession()
    }

    fun scanNextSpot() = startCurrentSpot()

    fun rescanCurrentSpot() {
        val idx = _currentSpotIndex.value
        if (idx > 0) {
            _spots.value = _spots.value.dropLast(1)
            _currentSpotIndex.value = idx - 1
        }
        startCurrentSpot()
    }

    private fun finishSession() {
        _phase.value = ScanPhase.RESULTS
        val session = ScanSession(
            id = UUID.randomUUID().toString().take(8),
            createdAtEpochMs = System.currentTimeMillis(),
            mode = _mode.value.label,
            spots = _spots.value,
            bestSpotLabel = ScoreEngine.bestSpot(_spots.value)?.label,
            optionsUsed = _scanOptions.value,
        )
        viewModelScope.launch {
            store.save(session)
            refreshHistory()
        }
        currentSession = session
    }

    var currentSession: ScanSession? = null
        private set

    fun sessionById(id: String, onLoaded: (ScanSession?) -> Unit) {
        viewModelScope.launch { onLoaded(_history.value.firstOrNull { it.id == id }) }
    }

    fun sessionsOnDay(dayStartMs: Long, dayEndMs: Long): List<ScanSession> =
        _history.value.filter { it.createdAtEpochMs in dayStartMs until dayEndMs }

    fun reset() {
        engine.cancel()
        _phase.value = ScanPhase.SETUP
        _spots.value = emptyList()
        _currentSpotIndex.value = 0
        _isFastScan.value = false
    }

    fun refreshHistory() {
        viewModelScope.launch { _history.value = store.list() }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch { store.delete(id); refreshHistory() }
    }

    fun clearHistory() {
        viewModelScope.launch { store.clear(); refreshHistory() }
    }

    override fun onCleared() = engine.cancel()
}
