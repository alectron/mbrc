package com.kelsos.mbrc.feature.settings.domain

import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.data.tags.CustomTagSuggestionDao
import com.kelsos.mbrc.core.data.tags.CustomTagSuggestionEntity
import com.kelsos.mbrc.core.networking.api.LibraryApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

interface TagMetadataSyncUseCase {
  suspend fun syncTags(): Result<Int>
}

class TagMetadataSyncUseCaseImpl(
  private val libraryApi: LibraryApi,
  private val suggestionDao: CustomTagSuggestionDao,
  private val settingsManager: SettingsManager,
  private val dispatchers: AppCoroutineDispatchers
) : TagMetadataSyncUseCase {

  override suspend fun syncTags(): Result<Int> = withContext(dispatchers.io) {
    try {
      val configuredTags = settingsManager.customTagFieldsFlow.first()
      val tagNames = (configuredTags.filter { it.isEnabled }.map { it.tag } + "genre")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }

      if (tagNames.isEmpty()) {
        return@withContext Result.success(0)
      }

      Timber.d("Syncing tag metadata for tags: $tagNames")
      val response = libraryApi.browseTagValues(tagNames)

      var totalInserted = 0
      val syncTimestamp = System.currentTimeMillis()

      for (entry in response.allEntries) {
        val tagName = entry.tag.trim()
        val values = entry.values.map { it.trim() }.filter { it.isNotBlank() }.distinct()

        if (values.isNotEmpty()) {
          val entities = values.map { value ->
            CustomTagSuggestionEntity(
              tag = tagName,
              value = value,
              dateAdded = syncTimestamp
            )
          }
          suggestionDao.insertAll(entities)
          totalInserted += entities.size
        }
        suggestionDao.removeOldEntriesForTag(tagName, syncTimestamp)
      }

      Timber.i("Tag metadata sync completed. Total values synced: $totalInserted")
      Result.success(totalInserted)
    } catch (e: Exception) {
      Timber.e(e, "Tag metadata sync failed")
      Result.failure(e)
    }
  }
}
