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
import com.spotkerja.settings.SettingsStore
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

    private val _mode = MutableStateFlow(WorkMode.WORK)
    val mode: StateFlow<WorkMode> = _mode

    private val _durationSec = MutableStateFlow(settings.durationSec)
    val durationSec: StateFlow<Int> = _durationSec

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

    val scanProgress: StateFlow<ScanProgress> = engine.progress

    init { refreshHistory() }

    fun setTheme(t: ThemeOption) { _theme.value = t; settings.theme = t }
    fun setMode(m: WorkMode) { _mode.value = m }
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
        _spots.value = emptyList()
        _currentSpotIndex.value = 0
        _phase.value = ScanPhase.SCANNING
        startCurrentSpot()
    }

    private fun startCurrentSpot() {
        val label = _spotNames.value.getOrNull(_currentSpotIndex.value) ?: return
        engine.start(viewModelScope, label, _durationSec.value, _mode.value) { result ->
            onSpotDone(result)
        }
    }

    fun stopCurrentSpotEarly() {
        val label = _spotNames.value.getOrNull(_currentSpotIndex.value) ?: return
        engine.stop(label, _durationSec.value, _mode.value) { result -> onSpotDone(result) }
    }

    private fun onSpotDone(result: SpotResult) {
        _spots.value = _spots.value + result
        val next = _currentSpotIndex.value + 1
        _currentSpotIndex.value = next
        if (next >= _spotNames.value.size) finishSession()
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
