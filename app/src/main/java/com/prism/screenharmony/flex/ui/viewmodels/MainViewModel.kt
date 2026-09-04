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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    BLOCK("Block", Icons.Rounded.Block),
    PARENTAL("Parenting", Icons.Rounded.FamilyRestroom),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

enum class ScreenState {
    MAIN_TABS,
    CREATE_OR_EDIT_BLOCK,
    SELECT_APPS,
    APP_LOCK_SETUP,
    RECOVERY_SETTINGS,
    DIAGNOSTICS_LOGS,
    ABOUT,
    PRIVACY_POLICY
}

data class PermissionState(
    val isUsageGranted: Boolean = true,
    val isOverlayGranted: Boolean = true,
    val isBatteryIgnored: Boolean = true,
    val isExactAlarmGranted: Boolean = true,
    val isAccessibilityGranted: Boolean = false,
    val isNotificationGranted: Boolean = true,
    val isMiuiDevice: Boolean = false,
    val isMiuiPopupGranted: Boolean = true
) {
    val hasCrucialPermissions: Boolean 
        get() = isUsageGranted && isOverlayGranted && isBatteryIgnored && (!isMiuiDevice || isMiuiPopupGranted)
    val areBase4PermissionsGranted: Boolean 
        get() = isUsageGranted && isOverlayGranted && isBatteryIgnored && isExactAlarmGranted && (!isMiuiDevice || isMiuiPopupGranted)
    val areAll5PermissionsGranted: Boolean 
        get() = areBase4PermissionsGranted && isAccessibilityGranted && isNotificationGranted
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val rules: StateFlow<List<BlockRule>> = BlockRepository.rules
        .map { list -> list.filter { !it.id.startsWith("remote_") } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentDestination = MutableStateFlow(AppDestinations.BLOCK)
    val currentDestination: StateFlow<AppDestinations> = _currentDestination.asStateFlow()

    private val _currentScreenState = MutableStateFlow(ScreenState.MAIN_TABS)
    val currentScreenState: StateFlow<ScreenState> = _currentScreenState.asStateFlow()

    private val _highlightPermissions = MutableStateFlow(false)
    val highlightPermissions: StateFlow<Boolean> = _highlightPermissions.asStateFlow()

    private val _highlightParentalControls = MutableStateFlow(false)
    val highlightParentalControls: StateFlow<Boolean> = _highlightParentalControls.asStateFlow()

    fun navigateToPermissionsSettings() {
        _currentDestination.value = AppDestinations.SETTINGS
        _currentScreenState.value = ScreenState.MAIN_TABS
        _highlightPermissions.value = true
    }

    fun clearPermissionsHighlight() {
        _highlightPermissions.value = false
    }

    fun navigateToParentalSettings() {
        _currentDestination.value = AppDestinations.SETTINGS
        _currentScreenState.value = ScreenState.MAIN_TABS
        _highlightParentalControls.value = true
    }

    fun clearParentalHighlight() {
        _highlightParentalControls.value = false
    }

    fun navigateToDiagnosticsLogs() {
        _currentScreenState.value = ScreenState.DIAGNOSTICS_LOGS
    }

    fun navigateToAbout() {
        _currentScreenState.value = ScreenState.ABOUT
    }

    fun navigateToPrivacyPolicy() {
        _currentScreenState.value = ScreenState.PRIVACY_POLICY
    }

    private val _editingRule = MutableStateFlow(BlockRule())
    val editingRule: StateFlow<BlockRule> = _editingRule.asStateFlow()

    private val _isAppListGridView = MutableStateFlow(false)
    val isAppListGridView: StateFlow<Boolean> = _isAppListGridView.asStateFlow()

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _isAppLocked = MutableStateFlow(AppLockManager.isAppLocked())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    val isOnlyParentMode: StateFlow<Boolean> = com.prism.screenharmony.flex.family.ParentalAuthManager.onlyParentModeFlow

    init {
        com.prism.screenharmony.flex.family.ParentalAuthManager.initialize(application)
        if (com.prism.screenharmony.flex.family.ParentalAuthManager.isOnlyParentMode(application)) {
            _currentDestination.value = AppDestinations.PARENTAL
        }
        refreshPermissions()
        checkAppLockState()

        // Periodic ticker to reactivate expired pause states in UI
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(5000)
                BlockRepository.cleanExpiredPauses()
            }
        }
    }

    fun setOnlyParentMode(enabled: Boolean) {
        val context = getApplication<Application>()
        com.prism.screenharmony.flex.family.ParentalAuthManager.setOnlyParentMode(context, enabled)
        if (enabled) {
            _currentDestination.value = AppDestinations.PARENTAL
        }
        com.prism.screenharmony.flex.service.BlockScheduleManager.reschedule(context)
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
            val exactAlarm = PermissionHelper.isExactAlarmGranted(context)
            val accessibility = PermissionHelper.isAccessibilityGranted(context)
            val notification = PermissionHelper.isNotificationGranted(context)
            val isMiui = PermissionHelper.isMiui()
            val miuiPopup = PermissionHelper.isMiuiBackgroundPopupGranted(context)
            withContext(Dispatchers.Main) {
                _permissionState.value = PermissionState(
                    isUsageGranted = usage,
                    isOverlayGranted = overlay,
                    isBatteryIgnored = battery,
                    isExactAlarmGranted = exactAlarm,
                    isAccessibilityGranted = accessibility,
                    isNotificationGranted = notification,
                    isMiuiDevice = isMiui,
                    isMiuiPopupGranted = miuiPopup
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
        val now = java.time.LocalTime.now()
        val day = java.time.DayOfWeek.from(java.time.LocalDate.now())
        val isStrictActive = (rule.pauseConfig.type == com.prism.screenharmony.flex.data.PauseType.STRICT || rule.blockType == com.prism.screenharmony.flex.data.BlockType.STRICT) && rule.isEnabled && !rule.isPaused() && rule.isCurrentlyBlocked(now, day)
        if (isStrictActive) return
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

    fun togglePause(rule: BlockRule, durationMinutes: Int = 5) {
        if (rule.isPaused()) {
            BlockRepository.unpauseRule(rule.id)
        } else {
            BlockRepository.pauseRule(rule.id, durationMinutes)
        }
    }

    fun handleBack(): Boolean {
        return when (_currentScreenState.value) {
            ScreenState.ABOUT -> {
                _currentScreenState.value = ScreenState.MAIN_TABS
                true
            }
            ScreenState.PRIVACY_POLICY -> {
                _currentScreenState.value = ScreenState.MAIN_TABS
                true
            }
            ScreenState.DIAGNOSTICS_LOGS -> {
                _currentScreenState.value = ScreenState.MAIN_TABS
                true
            }
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
