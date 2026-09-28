package com.kelsos.mbrc.feature.playback.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.core.common.settings.CustomTagFieldConfig
import com.kelsos.mbrc.core.common.state.AppStateFlow
import com.kelsos.mbrc.core.common.state.TrackDetails
import com.kelsos.mbrc.core.data.library.genre.GenreDao
import com.kelsos.mbrc.core.networking.protocol.actions.UserAction
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.usecases.UserActionUseCase
import com.kelsos.mbrc.core.networking.protocol.usecases.performUserAction
import com.kelsos.mbrc.core.networking.protocol.usecases.setTrackTag
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RatingDialogViewModel(
  private val userActionUseCase: UserActionUseCase,
  private val appState: AppStateFlow,
  private val settingsManager: SettingsManager,
  private val genreDao: GenreDao? = null
) : ViewModel() {
  private val _rating: MutableStateFlow<Float?> = MutableStateFlow(null)
  val rating: Flow<Float?> get() = _rating

  val halfStarEnabled: StateFlow<Boolean> = settingsManager.halfStarRatingFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val customTagFields: StateFlow<List<CustomTagFieldConfig>> = settingsManager.customTagFieldsFlow
    .map { list -> list.filter { it.isEnabled } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomTagFieldConfig.DEFAULT_TAGS)

  val trackDetails: StateFlow<TrackDetails> = appState.playingTrackDetails

  private val _genreSuggestions = MutableStateFlow<List<String>>(emptyList())
  val genreSuggestions: StateFlow<List<String>> = _genreSuggestions.asStateFlow()

  init {
    viewModelScope.launch {
      appState.playingTrackRating.map { it.rating }.distinctUntilChanged().collect {
        _rating.emit(it)
      }
    }
    viewModelScope.launch {
      userActionUseCase.perform(UserAction.create(Protocol.NowPlayingRating))
    }
    viewModelScope.launch {
      userActionUseCase.perform(UserAction.create(Protocol.NowPlayingDetails))
    }
    viewModelScope.launch(Dispatchers.IO) {
      try {
        genreDao?.genres()?.map { it.genre }?.let { list ->
          _genreSuggestions.emit(list.filter { it.isNotBlank() })
        }
      } catch (_: Exception) {
        // Ignored if DB not initialized or in unit tests
      }
    }
  }

  /**
   * Changes the rating for the current track.
   * @param rating The new rating: null = clear (send empty string), 0 = bomb, 0.5-5.0 = stars
   */
  fun changeRating(rating: Float?) {
    viewModelScope.launch {
      _rating.emit(rating)
      // Send empty string for clear (null), otherwise send the numeric value
      val payload: Any = rating ?: ""
      userActionUseCase.performUserAction(Protocol.NowPlayingRating, payload)
    }
  }

  /**
   * Changes a custom tag for the playing track.
   */
  fun changeTag(tagName: String, value: String) {
    viewModelScope.launch {
      userActionUseCase.setTrackTag(tagName, value)
    }
  }
}
