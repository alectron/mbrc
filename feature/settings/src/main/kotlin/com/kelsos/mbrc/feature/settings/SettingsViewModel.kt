package com.kelsos.mbrc.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.core.common.settings.CustomTagFieldConfig
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.core.platform.service.ServiceRestarter
import com.kelsos.mbrc.feature.settings.data.CallAction
import com.kelsos.mbrc.feature.settings.data.KeepScreenOn
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import com.kelsos.mbrc.feature.settings.theme.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Types of dialogs that can be shown in the Settings Screen.
 */
sealed class SettingsDialogType {
  data object Theme : SettingsDialogType()
  data object IncomingCallAction : SettingsDialogType()
  data object TrackDefaultAction : SettingsDialogType()
  data object KeepScreenOn : SettingsDialogType()
  data object AddCustomTagField : SettingsDialogType()
}

/**
 * ViewModel for the Settings screen.
 * Manages all settings-related state and business logic using proper ViewModel patterns.
 */
class SettingsViewModel(
  private val settingsManager: SettingsManager,
  private val serviceRestarter: ServiceRestarter
) : ViewModel() {

  // State flows from settings manager
  val currentTheme: StateFlow<Theme> = settingsManager.themeFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Theme.System)

  val pluginUpdatesEnabled: StateFlow<Boolean> = settingsManager.pluginUpdateCheckFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val debugLoggingEnabled: StateFlow<Boolean> = settingsManager.debugLoggingFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val incomingCallAction: StateFlow<CallAction> = settingsManager.incomingCallActionFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallAction.None)

  val trackDefaultAction: StateFlow<TrackAction> = settingsManager.libraryTrackDefaultActionFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackAction.PlayNow)

  val halfStarRatingEnabled: StateFlow<Boolean> = settingsManager.halfStarRatingFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val showRatingOnPlayerEnabled: StateFlow<Boolean> = settingsManager.showRatingOnPlayerFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val keepScreenOn: StateFlow<KeepScreenOn> = settingsManager.keepScreenOnFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KeepScreenOn.Never)

  val customTagFields: StateFlow<List<CustomTagFieldConfig>> = settingsManager.customTagFieldsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomTagFieldConfig.DEFAULT_TAGS)

  // Dialog state
  private val _visibleDialog = MutableStateFlow<SettingsDialogType?>(null)
  val visibleDialog: StateFlow<SettingsDialogType?> = _visibleDialog.asStateFlow()

  /**
   * Updates the app theme.
   */
  fun updateTheme(theme: Theme) {
    viewModelScope.launch {
      settingsManager.setTheme(theme)
    }
  }

  /**
   * Updates plugin update check preference.
   */
  fun updatePluginUpdates(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.setPluginUpdateCheck(enabled)
    }
  }

  /**
   * Updates the debug logging preference.
   *
   * Storing it is the whole job: the settings data store watches the value and owns the logging
   * tree. Applying it here as well meant two writers racing over one log file.
   */
  fun updateDebugLogging(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.setDebugLogging(enabled)
    }
  }

  /**
   * Updates incoming call action preference and restarts service.
   */
  fun updateIncomingCallAction(action: CallAction) {
    viewModelScope.launch {
      settingsManager.setIncomingCallAction(action)
      serviceRestarter.restartService()
    }
  }

  /**
   * Updates track default action preference.
   */
  fun updateTrackDefaultAction(action: TrackAction) {
    viewModelScope.launch {
      settingsManager.setLibraryTrackDefaultAction(action)
    }
  }

  /**
   * Updates half-star rating preference.
   */
  fun updateHalfStarRating(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.setHalfStarRating(enabled)
    }
  }

  /**
   * Updates show rating on player preference.
   */
  fun updateShowRatingOnPlayer(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.setShowRatingOnPlayer(enabled)
    }
  }

  /**
   * Updates whether the screen stays awake while the app is visible.
   */
  fun updateKeepScreenOn(mode: KeepScreenOn) {
    viewModelScope.launch {
      settingsManager.setKeepScreenOn(mode)
    }
  }

  /**
   * Updates the list of custom tag fields.
   */
  fun updateCustomTagFields(fields: List<CustomTagFieldConfig>) {
    viewModelScope.launch {
      settingsManager.setCustomTagFields(fields)
    }
  }

  /**
   * Adds a new custom tag field if not already present.
   */
  fun addCustomTagField(tag: String, isMultiValue: Boolean) {
    val trimmed = tag.trim()
    if (trimmed.isEmpty()) return
    val current = customTagFields.value
    if (current.any { it.tag.equals(trimmed, ignoreCase = true) }) return
    val updated = current + CustomTagFieldConfig(tag = trimmed, isMultiValue = isMultiValue, isEnabled = true)
    updateCustomTagFields(updated)
  }

  /**
   * Removes a custom tag field at the given index.
   */
  fun removeCustomTagField(index: Int) {
    val current = customTagFields.value.toMutableList()
    if (index in current.indices) {
      current.removeAt(index)
      updateCustomTagFields(current)
    }
  }

  /**
   * Toggles the enabled state of a custom tag field at the given index.
   */
  fun toggleCustomTagField(index: Int) {
    val current = customTagFields.value.toMutableList()
    if (index in current.indices) {
      val item = current[index]
      current[index] = item.copy(isEnabled = !item.isEnabled)
      updateCustomTagFields(current)
    }
  }

  /**
   * Shows a dialog of the specified type.
   */
  fun showDialog(dialogType: SettingsDialogType) {
    _visibleDialog.value = dialogType
  }

  /**
   * Hides the currently visible dialog.
   */
  fun hideDialog() {
    _visibleDialog.value = null
  }
}
