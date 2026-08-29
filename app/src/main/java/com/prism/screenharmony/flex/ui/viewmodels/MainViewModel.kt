package com.prism.screenharmony.flex.ui.viewmodels

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prism.screenharmony.flex.data.AppLockManager
import com.prism.screenharmony.flex.data.BlockRepository
import com.prism.screenharmony.flex.data.BlockRule
import com.prism.screenharmony.flex.utils.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    BLOCK("Block", Icons.Rounded.Block),
    PARENTAL("Parental", Icons.Rounded.FamilyRestroom),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

enum class ScreenState {
    MAIN_TABS,
    CREATE_OR_EDIT_BLOCK,
    SELECT_APPS,
    APP_LOCK_SETUP,
    RECOVERY_SETTINGS
}

data class PermissionState(
    val isUsageGranted: Boolean = true,
    val isOverlayGranted: Boolean = true,
    val isBatteryIgnored: Boolean = true,
    val isAccessibilityGranted: Boolean = false
) {
    val hasCrucialPermissions: Boolean get() = isUsageGranted && isOverlayGranted
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val rules: StateFlow<List<BlockRule>> = BlockRepository.rules

    private val _currentDestination = MutableStateFlow(AppDestinations.BLOCK)
    val currentDestination: StateFlow<AppDestinations> = _currentDestination.asStateFlow()

    private val _currentScreenState = MutableStateFlow(ScreenState.MAIN_TABS)
    val currentScreenState: StateFlow<ScreenState> = _currentScreenState.asStateFlow()

    private val _editingRule = MutableStateFlow(BlockRule())
    val editingRule: StateFlow<BlockRule> = _editingRule.asStateFlow()

    private val _isAppListGridView = MutableStateFlow(false)
    val isAppListGridView: StateFlow<Boolean> = _isAppListGridView.asStateFlow()

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _isAppLocked = MutableStateFlow(AppLockManager.isAppLocked())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    init {
        refreshPermissions()
        checkAppLockState()
    }

    fun checkAppLockState() {
        _isAppLocked.value = AppLockManager.isAppLocked()
    }

    fun onAppForegrounded() {
        AppLockManager.onAppForegrounded()
        checkAppLockState()
        refreshPermissions()
    }

    fun onAppUnlocked() {
        AppLockManager.unlockSession()
        _isAppLocked.value = false
    }

    fun openAppLockSetup() {
        _currentScreenState.value = ScreenState.APP_LOCK_SETUP
    }

    fun openRecoverySettings() {
        _currentScreenState.value = ScreenState.RECOVERY_SETTINGS
    }

    fun onAppLockSetupComplete() {
        _currentScreenState.value = ScreenState.MAIN_TABS
        _isAppLocked.value = false
    }

    fun refreshPermissions() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val usage = PermissionHelper.isUsageAccessGranted(context)
            val overlay = PermissionHelper.isOverlayGranted(context)
            val battery = PermissionHelper.isBatteryOptimizationIgnored(context)
            val accessibility = PermissionHelper.isAccessibilityGranted(context)
            withContext(Dispatchers.Main) {
                _permissionState.value = PermissionState(
                    isUsageGranted = usage,
                    isOverlayGranted = overlay,
                    isBatteryIgnored = battery,
                    isAccessibilityGranted = accessibility
                )
            }
        }
    }

    fun setDestination(destination: AppDestinations) {
        _currentDestination.value = destination
    }

    fun openCreateRule() {
        _editingRule.value = BlockRule()
        _currentScreenState.value = ScreenState.CREATE_OR_EDIT_BLOCK
    }

    fun openEditRule(rule: BlockRule) {
        _editingRule.value = rule
        _currentScreenState.value = ScreenState.CREATE_OR_EDIT_BLOCK
    }

    fun openSelectApps() {
        _currentScreenState.value = ScreenState.SELECT_APPS
    }

    fun updateEditingRule(rule: BlockRule) {
        _editingRule.value = rule
    }

    fun saveEditingRule() {
        val rule = _editingRule.value
        val finalRule = if (rule.name.isBlank()) rule.copy(name = "App Block") else rule
        BlockRepository.addOrUpdateRule(finalRule)
        _currentScreenState.value = ScreenState.MAIN_TABS
    }

    fun onAppsSelected(apps: Set<String>) {
        _editingRule.value = _editingRule.value.copy(selectedApps = apps)
        _currentScreenState.value = ScreenState.CREATE_OR_EDIT_BLOCK
    }

    fun setAppListGridView(isGrid: Boolean) {
        _isAppListGridView.value = isGrid
    }

    fun toggleRule(ruleId: String, isEnabled: Boolean) {
        BlockRepository.toggleRule(ruleId, isEnabled)
    }

    fun deleteRule(ruleId: String) {
        BlockRepository.deleteRule(ruleId)
    }

    fun togglePause(rule: BlockRule) {
        if (rule.isPaused()) {
            BlockRepository.unpauseRule(rule.id)
        } else {
            BlockRepository.pauseRule(rule.id, 5)
        }
    }

    fun handleBack(): Boolean {
        return when (_currentScreenState.value) {
            ScreenState.APP_LOCK_SETUP -> {
                _currentScreenState.value = ScreenState.MAIN_TABS
                true
            }
            ScreenState.RECOVERY_SETTINGS -> {
                _currentScreenState.value = ScreenState.MAIN_TABS
                true
            }
            ScreenState.SELECT_APPS -> {
                _currentScreenState.value = ScreenState.CREATE_OR_EDIT_BLOCK
                true
            }
            ScreenState.CREATE_OR_EDIT_BLOCK -> {
                _currentScreenState.value = ScreenState.MAIN_TABS
                true
            }
            ScreenState.MAIN_TABS -> {
                if (_currentDestination.value != AppDestinations.BLOCK) {
                    _currentDestination.value = AppDestinations.BLOCK
                    true
                } else {
                    false // allow OS back to minimize app
                }
            }
        }
    }
}
