package com.hidra.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hidra.launcher.data.AppInfo
import com.hidra.launcher.data.AppRepository
import com.hidra.launcher.data.Disguise
import com.hidra.launcher.data.DisguiseManager
import com.hidra.launcher.data.Override
import com.hidra.launcher.data.PrefsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val prefs = PrefsRepository(app)
    private val disguiseManager = DisguiseManager(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val apps = withContext(Dispatchers.IO) { appRepo.load() }
            _state.value = UiState(
                loading = false,
                apps = apps,
                disguise = disguiseManager.current(),
                hasPin = prefs.hasPin(),
            )
        }
    }

    fun launch(app: AppInfo) = appRepo.launch(app)

    fun updateOverride(app: AppInfo, override: Override) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { prefs.saveOverride(app.key, override) }
            refresh()
        }
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
}
