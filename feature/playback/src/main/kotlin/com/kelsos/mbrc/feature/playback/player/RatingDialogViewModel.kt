package com.kelsos.mbrc.feature.playback.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.core.common.settings.CustomTagFieldConfig
import com.kelsos.mbrc.core.common.state.AppStateFlow
import com.kelsos.mbrc.core.common.state.TrackDetails
import com.kelsos.mbrc.core.data.library.genre.GenreDao
import com.kelsos.mbrc.core.data.tags.CustomTagSuggestionDao
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

class RatingDialogViewModel(
  private val userActionUseCase: UserActionUseCase,
  private val appState: AppStateFlow,
  private val settingsManager: SettingsManager,
  private val genreDao: GenreDao? = null,
  private val suggestionDao: CustomTagSuggestionDao? = null,
  private val recentTagsStore: RecentTagsStore? = null
) : ViewModel() {
  private val _rating: MutableStateFlow<Float?> = MutableStateFlow(null)
  val rating: Flow<Float?> get() = _rating

  val halfStarEnabled: StateFlow<Boolean> = settingsManager.halfStarRatingFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val customTagFields: StateFlow<List<CustomTagFieldConfig>> = settingsManager.customTagFieldsFlow
    .map { list -> list.filter { it.isEnabled } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomTagFieldConfig.DEFAULT_TAGS)

  val tagSuggestionLimit: StateFlow<Int> = settingsManager.tagSuggestionLimitFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)

  private val _confirmedTags = MutableStateFlow<Set<String>>(emptySet())
  val confirmedTags: StateFlow<Set<String>> = _confirmedTags.asStateFlow()

  private val _optimisticTags = MutableStateFlow<Map<String, String>>(emptyMap())
  private val _trackDetails = MutableStateFlow(appState.playingTrackDetails.value)
  val trackDetails: StateFlow<TrackDetails> get() = _trackDetails.asStateFlow()

  private val _genreSuggestions = MutableStateFlow<List<String>>(emptyList())
  val genreSuggestions: StateFlow<List<String>> = _genreSuggestions.asStateFlow()

  private val _tagSuggestionsMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
  val tagSuggestionsMap: StateFlow<Map<String, List<String>>> = _tagSuggestionsMap.asStateFlow()

  init {
    viewModelScope.launch {
      appState.playingTrackRating.map { it.rating }.distinctUntilChanged().collect {
        _rating.emit(it)
      }
    }
    viewModelScope.launch {
      appState.playingTrackDetails.collect { serverDetails ->
        var current = serverDetails
        for ((tag, value) in _optimisticTags.value) {
          current = current.withTagValue(tag, value)
        }
        _trackDetails.value = current
      }
    }
    viewModelScope.launch {
      appState.playingTrack.collect {
        _optimisticTags.value = emptyMap()
        _trackDetails.value = appState.playingTrackDetails.value
      }
    }
    viewModelScope.launch {
      userActionUseCase.perform(UserAction.create(Protocol.NowPlayingRating))
    }
    viewModelScope.launch {
      userActionUseCase.perform(UserAction.create(Protocol.NowPlayingDetails))
    }
    viewModelScope.launch(Dispatchers.IO) {
      loadConfirmedTags()
      refreshAllSuggestions()

      val confirmedFlow = suggestionDao?.getAllConfirmedTagValuesFlow() ?: flowOf(emptyList())
      combine(
        customTagFields,
        tagSuggestionLimit,
        confirmedFlow
      ) { _, _, _ -> }.collect {
        loadConfirmedTags()
        refreshAllSuggestions()
      }
    }
  }

  private fun loadConfirmedTags() {
    try {
      val set = mutableSetOf<String>()
      suggestionDao?.getAllConfirmedTagValues()?.let { set.addAll(it) }
      genreDao?.genres()?.forEach {
        if (it.genre.isNotBlank()) {
          set.add("genre:${it.genre.trim().lowercase()}")
        }
      }
      _confirmedTags.value = set
    } catch (_: Exception) {
      // Ignored if DB not initialized or in unit tests
    }
  }

  private fun computeSuggestions(tag: String, limit: Int): List<String> {
    val normalizedTag = tag.trim().lowercase()
    val recent = recentTagsStore?.getRecentTags(normalizedTag) ?: emptyList()

    val fromDb = try {
      val list = mutableListOf<String>()
      suggestionDao?.getSuggestionsForTag(normalizedTag)?.let { list.addAll(it) }
      if (normalizedTag == "genre") {
        genreDao?.genres()?.map { it.genre }?.let { list.addAll(it) }
      }
      list.filter { it.isNotBlank() }.distinct()
    } catch (_: Exception) {
      emptyList()
    }

    val result = mutableListOf<String>()
    val seen = mutableSetOf<String>()

    for (item in recent) {
      val lower = item.lowercase()
      if (seen.add(lower)) {
        result.add(item)
        if (result.size >= limit) return result
      }
    }

    for (item in fromDb) {
      val lower = item.lowercase()
      if (seen.add(lower)) {
        result.add(item)
        if (result.size >= limit) break
      }
    }

    return result
  }

  private suspend fun refreshAllSuggestions() = withContext(Dispatchers.IO) {
    val limit = tagSuggestionLimit.value
    val tags = (customTagFields.value.map { it.tag } + "genre")
      .map { it.trim().lowercase() }
      .distinct()
    val map = mutableMapOf<String, List<String>>()
    for (tag in tags) {
      map[tag] = computeSuggestions(tag, limit)
    }
    _tagSuggestionsMap.value = map
    _genreSuggestions.value = map["genre"] ?: emptyList()
  }

  /**
   * Returns merged suggestions for the given tag:
   * 1. In-memory cache from IO thread first (safe on Main Thread)
   * 2. Fallback to direct computation
   */
  fun getSuggestionsForTag(tag: String): List<String> {
    val normalized = tag.trim().lowercase()
    val cached = _tagSuggestionsMap.value[normalized]
    if (cached != null && cached.isNotEmpty()) {
      return cached
    }
    return computeSuggestions(tag, tagSuggestionLimit.value)
  }

  /**
   * Checks whether a tag value is confirmed in MusicBee / SQLite cache.
   */
  fun isTagConfirmed(tag: String, value: String): Boolean {
    val key = "${tag.trim()}:${value.trim()}".lowercase()
    return _confirmedTags.value.contains(key)
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
    _optimisticTags.update { it + (tagName to value) }
    _trackDetails.value = _trackDetails.value.withTagValue(tagName, value)
    viewModelScope.launch(Dispatchers.IO) {
      value.split(";").forEach { piece ->
        val trimmed = piece.trim()
        if (trimmed.isNotBlank()) {
          recentTagsStore?.recordTagUsed(tagName, trimmed)
        }
      }
      refreshAllSuggestions()
      userActionUseCase.setTrackTag(tagName, value)
    }
  }
}
