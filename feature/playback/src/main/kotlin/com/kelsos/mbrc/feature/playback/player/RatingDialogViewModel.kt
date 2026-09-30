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

import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

data class TagClipboardEntry(
  val tagName: String,
  val values: List<String>,
  val remainingTtl: Int = 10
)

data class PendingReorderEntry(
  val tagName: String,
  val values: List<String>
)

class RatingDialogViewModel(
  private val userActionUseCase: UserActionUseCase,
  private val appState: AppStateFlow,
  private val settingsManager: SettingsManager,
  private val genreDao: GenreDao? = null,
  private val suggestionDao: CustomTagSuggestionDao? = null,
  private val recentTagsStore: RecentTagsStore? = null,
  private val dispatchers: AppCoroutineDispatchers? = null
) : ViewModel() {
  private val ioDispatcher = dispatchers?.io ?: Dispatchers.IO
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

  private val _allTagValuesMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
  val allTagValuesMap: StateFlow<Map<String, List<String>>> = _allTagValuesMap.asStateFlow()

  private val _tagClipboard = MutableStateFlow<Map<String, TagClipboardEntry>>(emptyMap())
  val tagClipboard: StateFlow<Map<String, TagClipboardEntry>> = _tagClipboard.asStateFlow()

  private val _pendingReorders = MutableStateFlow<Map<String, PendingReorderEntry>>(emptyMap())
  val pendingReorders: StateFlow<Map<String, PendingReorderEntry>> = _pendingReorders.asStateFlow()

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
    var lastTrackId: String? = null
    viewModelScope.launch {
      appState.playingTrack.collect { track ->
        _optimisticTags.value = emptyMap()
        _trackDetails.value = appState.playingTrackDetails.value

        val currentId = track.path.ifBlank { "${track.artist}:${track.title}" }
        if (lastTrackId != null && lastTrackId != currentId) {
          _tagClipboard.update { current ->
            current.mapNotNull { (key, entry) ->
              val nextTtl = entry.remainingTtl - 1
              if (nextTtl > 0) {
                key to entry.copy(remainingTtl = nextTtl)
              } else {
                null
              }
            }.toMap()
          }
        }
        if (currentId.isNotBlank()) {
          lastTrackId = currentId
        }
      }
    }
    viewModelScope.launch {
      userActionUseCase.perform(UserAction.create(Protocol.NowPlayingRating))
    }
    viewModelScope.launch {
      userActionUseCase.perform(UserAction.create(Protocol.NowPlayingDetails))
    }
    viewModelScope.launch(ioDispatcher) {
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
      _confirmedTags.value = set
    } catch (_: Exception) {
      // Ignored if DB not initialized or in unit tests
    }
  }

  private fun computeQuickSuggestions(tag: String, listFromDb: List<String>, limit: Int): List<String> {
    val normalizedTag = tag.trim().lowercase()
    val recent = recentTagsStore?.getRecentTags(normalizedTag) ?: emptyList()

    val result = mutableListOf<String>()
    val seen = mutableSetOf<String>()

    for (item in recent) {
      val lower = item.lowercase()
      if (seen.add(lower)) {
        result.add(item)
        if (result.size >= limit) return result
      }
    }

    for (item in listFromDb) {
      val lower = item.lowercase()
      if (seen.add(lower)) {
        result.add(item)
        if (result.size >= limit) break
      }
    }

    return result
  }

  private suspend fun refreshAllSuggestions() = withContext(ioDispatcher) {
    val limit = tagSuggestionLimit.value
    val tags = customTagFields.value.map { it.tag }
      .map { it.trim().lowercase() }
      .filter { it.isNotBlank() }
      .distinct()
    val quickMap = mutableMapOf<String, List<String>>()
    val allMap = mutableMapOf<String, List<String>>()

    for (tag in tags) {
      val listFromDb = try {
        suggestionDao?.getSuggestionsForTag(tag)?.filter { it.isNotBlank() }?.distinct() ?: emptyList()
      } catch (_: Exception) {
        emptyList()
      }
      allMap[tag] = listFromDb
      quickMap[tag] = computeQuickSuggestions(tag, listFromDb, limit)
    }

    // Prune stale tag history for removed tag fields
    recentTagsStore?.pruneStaleTags(tags.toSet())

    _allTagValuesMap.value = allMap
    _tagSuggestionsMap.value = quickMap
    _genreSuggestions.value = quickMap["genre"] ?: emptyList()
  }

  /**
   * Returns quick suggestions for the given tag:
   * Uses in-memory cache safely on the Main Thread.
   */
  fun getSuggestionsForTag(tag: String): List<String> {
    val normalized = tag.trim().lowercase()
    val cached = _tagSuggestionsMap.value[normalized]
    if (cached != null && cached.isNotEmpty()) {
      return cached
    }
    val inMemoryValues = _allTagValuesMap.value[normalized]
    if (inMemoryValues != null && inMemoryValues.isNotEmpty()) {
      return computeQuickSuggestions(tag, inMemoryValues, tagSuggestionLimit.value)
    }

    // Direct DB query fallback with safety protection:
    // If called on Android Main Thread where Room prohibits main-thread queries,
    // catch IllegalStateException and return recent suggestions safely instead of crashing.
    val dbValues = try {
      suggestionDao?.getSuggestionsForTag(normalized)?.filter { it.isNotBlank() }?.distinct() ?: emptyList()
    } catch (_: IllegalStateException) {
      emptyList()
    } catch (_: Exception) {
      emptyList()
    }

    return computeQuickSuggestions(tag, dbValues, tagSuggestionLimit.value)
  }

  /**
   * Returns all known values for the given tag (for dialog search/autocomplete).
   */
  fun getAllValuesForTag(tag: String): List<String> {
    val normalized = tag.trim().lowercase()
    return _allTagValuesMap.value[normalized] ?: emptyList()
  }

  /**
   * Checks whether a tag value is confirmed in MusicBee / SQLite cache or on the active track.
   */
  fun isTagConfirmed(tag: String, value: String): Boolean {
    val key = "${tag.trim()}:${value.trim()}".lowercase()
    if (_confirmedTags.value.contains(key)) return true

    // Check if the current playing track details already contains this tag value
    val currentTrackVal = _trackDetails.value.getTagValue(tag)
    if (currentTrackVal.isNotBlank()) {
      val values = currentTrackVal.split(';').map { it.trim().lowercase() }
      if (values.contains(value.trim().lowercase())) {
        return true
      }
    }
    return false
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
    val cleanTagName = tagName.trim()
    _optimisticTags.update { it + (cleanTagName to value) }
    _trackDetails.value = _trackDetails.value.withTagValue(cleanTagName, value)
    _pendingReorders.update { it - cleanTagName.lowercase() }

    // Immediately mark saved values as confirmed so newly entered chips are styled confirmed
    val newKeys = value.split(";")
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .map { "${cleanTagName}:${it}".lowercase() }
    if (newKeys.isNotEmpty()) {
      _confirmedTags.update { it + newKeys }
    }

    viewModelScope.launch(ioDispatcher) {
      value.split(";").forEach { piece ->
        val trimmed = piece.trim()
        if (trimmed.isNotBlank()) {
          recentTagsStore?.recordTagUsed(cleanTagName, trimmed)
        }
      }
      refreshAllSuggestions()
      userActionUseCase.setTrackTag(cleanTagName, value)
    }
  }

  /**
   * Stages a reordered list of tags in local UI state without immediately committing across the network.
   * Shows the push icon (->) and updates the local optimistic details.
   */
  fun stageReorder(tagName: String, newValues: List<String>) {
    val cleanTagName = tagName.trim()
    val normalized = cleanTagName.lowercase()
    _pendingReorders.update { it + (normalized to PendingReorderEntry(cleanTagName, newValues)) }
    val joined = newValues.joinToString("; ")
    _optimisticTags.update { it + (cleanTagName to joined) }
    _trackDetails.value = _trackDetails.value.withTagValue(cleanTagName, joined)
  }

  /**
   * Commits the pending reorder for a specific tag to MusicBee.
   */
  fun commitPendingReorder(tagName: String) {
    val normalized = tagName.trim().lowercase()
    val entry = _pendingReorders.value[normalized] ?: return
    _pendingReorders.update { it - normalized }
    reorderTags(entry.tagName, entry.values)
  }

  /**
   * Commits all pending staged reorders (called when bottom sheet dismisses).
   */
  fun commitAllPendingReorders() {
    val current = _pendingReorders.value
    if (current.isEmpty()) return
    _pendingReorders.value = emptyMap()
    for ((_, entry) in current) {
      reorderTags(entry.tagName, entry.values)
    }
  }

  fun hasPendingReorder(tagName: String): Boolean {
    val normalized = tagName.trim().lowercase()
    return _pendingReorders.value.containsKey(normalized)
  }

  /**
   * Reorders tag values for a custom tag and commits the new order.
   */
  fun reorderTags(tagName: String, newValues: List<String>) {
    val joined = newValues.joinToString("; ")
    changeTag(tagName, joined)
  }

  /**
   * Copies active values of a tag field into the in-memory clipboard cache with a 10-track TTL.
   */
  fun copyTags(tagName: String, values: List<String>) {
    if (values.isEmpty()) return
    val normalized = tagName.trim().lowercase()
    _tagClipboard.update { current ->
      current + (normalized to TagClipboardEntry(tagName = tagName, values = values, remainingTtl = 10))
    }
  }

  /**
   * Pastes/merges cached tag values from clipboard for the specified tag field onto the current track.
   */
  fun pasteTags(tagName: String) {
    val normalized = tagName.trim().lowercase()
    val entry = _tagClipboard.value[normalized] ?: return
    val currentRaw = _trackDetails.value.getTagValue(tagName)
    val currentList = currentRaw.split(";")
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .toMutableList()

    var changed = false
    for (clipVal in entry.values) {
      if (currentList.none { it.equals(clipVal, ignoreCase = true) }) {
        currentList.add(clipVal)
        changed = true
      }
    }
    if (changed) {
      changeTag(tagName, currentList.joinToString("; "))
    }
  }

  /**
   * Clears the in-memory tag clipboard.
   */
  fun clearTagClipboard() {
    _tagClipboard.value = emptyMap()
  }
}
