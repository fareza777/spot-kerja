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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Fase flow scan: memilih mode → scan tiap spot → hasil. */
enum class ScanPhase { SETUP, SCANNING, RESULTS }

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SessionStore(app)
    val engine = ScanEngine(app)

    private val _mode = MutableStateFlow(WorkMode.WORK)
    val mode: StateFlow<WorkMode> = _mode

    private val _durationSec = MutableStateFlow(30)
    val durationSec: StateFlow<Int> = _durationSec

    private val _phase = MutableStateFlow(ScanPhase.SETUP)
    val phase: StateFlow<ScanPhase> = _phase

    /** Label spot berikutnya: A, B, C, ... */
    private val _spotLabels = MutableStateFlow(listOf("A", "B", "C"))
    val spotLabels: StateFlow<List<String>> = _spotLabels

    private val _currentSpotIndex = MutableStateFlow(0)
    val currentSpotIndex: StateFlow<Int> = _currentSpotIndex

    private val _spots = MutableStateFlow<List<SpotResult>>(emptyList())
    val spots: StateFlow<List<SpotResult>> = _spots

    private val _history = MutableStateFlow<List<ScanSession>>(emptyList())
    val history: StateFlow<List<ScanSession>> = _history

    private val _sessionSaved = MutableStateFlow(false)
    val sessionSaved: StateFlow<Boolean> = _sessionSaved

    val scanProgress: StateFlow<ScanProgress> = engine.progress

    init { refreshHistory() }

    fun setMode(m: WorkMode) { _mode.value = m }
    fun setDuration(sec: Int) { _durationSec.value = sec.coerceIn(30, 60) }

    fun addSpotLabel() {
        val next = ('A' + _spotLabels.value.size)
        if (next <= 'F') _spotLabels.value = _spotLabels.value + next.toString()
    }

    fun removeSpotLabel(i: Int) {
        if (_spotLabels.value.size > 2) _spotLabels.value = _spotLabels.value.toMutableList().also { it.removeAt(i) }
    }

    fun startScan() {
        _spots.value = emptyList()
        _currentSpotIndex.value = 0
        _sessionSaved.value = false
        _phase.value = ScanPhase.SCANNING
        startCurrentSpot()
    }

    private fun startCurrentSpot() {
        val label = _spotLabels.value.getOrNull(_currentSpotIndex.value) ?: return
        engine.start(viewModelScope, label, _durationSec.value, _mode.value) { result ->
            onSpotDone(result)
        }
    }

    fun stopCurrentSpotEarly() {
        val label = _spotLabels.value.getOrNull(_currentSpotIndex.value) ?: return
        engine.stop(label, _durationSec.value, _mode.value) { result -> onSpotDone(result) }
    }

    private fun onSpotDone(result: SpotResult) {
        _spots.value = _spots.value + result
        val next = _currentSpotIndex.value + 1
        _currentSpotIndex.value = next
        if (next >= _spotLabels.value.size) {
            finishSession()
        }
        // kalau belum selesai, user menekan "Scan Spot X" di layar scan
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
            _sessionSaved.value = true
            refreshHistory()
        }
        currentSession = session
    }

    var currentSession: ScanSession? = null
        private set

    fun sessionById(id: String, onLoaded: (ScanSession?) -> Unit) {
        viewModelScope.launch {
            onLoaded(_history.value.firstOrNull { it.id == id })
        }
    }

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
