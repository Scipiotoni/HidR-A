package com.hidra.launcher.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hidra.launcher.data.AppInfo
import com.hidra.launcher.data.AppRepository
import com.hidra.launcher.data.Disguise
import com.hidra.launcher.data.DisguiseManager
import com.hidra.launcher.data.Override
import com.hidra.launcher.data.PrefsRepository
import com.hidra.launcher.shizuku.ShizukuController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class UiState(
    val loading: Boolean = true,
    val apps: List<AppInfo> = emptyList(),
    val disguise: Disguise = Disguise.Default,
    val hasPin: Boolean = false,
) {
    val visibleApps: List<AppInfo> get() = apps.filterNot { it.isHidden }
    val hiddenApps: List<AppInfo> get() = apps.filter { it.isHidden }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val appRepo = AppRepository(app)
    private val hiddenStore get() = appRepo.hiddenStore
    private val prefs = PrefsRepository(app)
    private val disguiseManager = DisguiseManager(app)
    private val refreshLock = Mutex()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        ShizukuController.init(app)
        refresh()
        // Once Shizuku becomes usable, disable any apps that were hidden before it was.
        viewModelScope.launch {
            ShizukuController.status.collect { status ->
                if (status == ShizukuController.Status.Ready) refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshLock.withLock {
                _state.value = _state.value.copy(loading = true)
                val apps = withContext(Dispatchers.IO) {
                    var list = appRepo.load()
                    if (ShizukuController.isReady && syncFrozen(list)) list = appRepo.load()
                    list
                }
                _state.value = UiState(
                    loading = false,
                    apps = apps,
                    disguise = disguiseManager.current(),
                    hasPin = prefs.hasPin(),
                )
            }
        }
    }

    /**
     * Any hidden app that still resolves is currently enabled (never frozen, or
     * just opened from the Hidden list), so disable it again. Returns true if
     * anything changed.
     */
    private fun syncFrozen(apps: List<AppInfo>): Boolean {
        var changed = false
        for (a in apps) {
            if (a.isHidden && !a.frozen) {
                hiddenStore.save(a)
                if (ShizukuController.freeze(a.packageName)) changed = true
            }
        }
        return changed
    }

    fun launch(app: AppInfo) {
        viewModelScope.launch {
            if (app.isHidden && app.frozen) {
                if (!ShizukuController.isReady) {
                    toast("Start Shizuku to open hidden apps.")
                    return@launch
                }
                val restored = withContext(Dispatchers.IO) { ShizukuController.unfreeze(app.packageName) }
                if (!restored) {
                    toast("Couldn't unlock ${app.displayLabel}.")
                    return@launch
                }
                delay(300)
            }
            if (!appRepo.launch(app)) toast("Couldn't open ${app.displayLabel}.")
        }
    }

    fun updateOverride(app: AppInfo, override: Override) {
        viewModelScope.launch {
            val error = withContext(Dispatchers.IO) { applyHiddenChange(app, override) }
            if (error != null) {
                toast(error)
                return@launch
            }
            withContext(Dispatchers.IO) { prefs.saveOverride(app.key, override) }
            refresh()
        }
    }

    /** Freezes or restores the app when its hidden flag flips. Returns an error message, or null. */
    private fun applyHiddenChange(app: AppInfo, new: Override): String? {
        val ready = ShizukuController.isReady
        if (new.hidden && !app.override.hidden) {
            hiddenStore.save(app)
            if (ready && !ShizukuController.freeze(app.packageName)) {
                hiddenStore.remove(app.key)
                return "Couldn't hide ${app.displayLabel} system-wide."
            }
        } else if (!new.hidden && app.override.hidden) {
            if (app.frozen) {
                if (!ready) return "Start Shizuku first. ${app.displayLabel} is disabled and needs it to be restored."
                if (!ShizukuController.unfreeze(app.packageName)) {
                    return "Couldn't restore ${app.displayLabel}."
                }
            }
            hiddenStore.remove(app.key)
        }
        return null
    }

    fun setHidden(app: AppInfo, hidden: Boolean) =
        updateOverride(app, app.override.copy(hidden = hidden))

    fun applyDisguise(disguise: Disguise) {
        disguiseManager.apply(disguise)
        _state.value = _state.value.copy(disguise = disguise)
    }

    // ---- PIN -----------------------------------------------------------
    fun hasPin(): Boolean = prefs.hasPin()
    fun setPin(pin: String) {
        prefs.setPin(pin)
        _state.value = _state.value.copy(hasPin = true)
    }
    fun checkPin(pin: String): Boolean = prefs.checkPin(pin)

    private fun toast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
    }
}
