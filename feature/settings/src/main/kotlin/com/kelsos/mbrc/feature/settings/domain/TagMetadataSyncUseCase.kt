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
  fun getCachedTagCountFlow(): kotlinx.coroutines.flow.Flow<Int>
  suspend fun getCachedTagCount(): Int
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
      val enabledTags = configuredTags.filter { it.isEnabled }
      val tagNames = enabledTags.map { it.tag.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }

      if (tagNames.isEmpty()) {
        Timber.d("No enabled custom tag fields to sync. Flushing custom tag cache.")
        suggestionDao.clearAll()
        return@withContext Result.success(0)
      }

      Timber.d("Syncing tag metadata for tags: $tagNames")
      val response = libraryApi.browseTagValues(tagNames)

      // Query succeeded: atomically flush the old cache and replace with fresh values
      suggestionDao.clearAll()

      var totalInserted = 0
      val syncTimestamp = System.currentTimeMillis()

      for (entry in response.allEntries) {
        val tagName = entry.tag.trim().lowercase()
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
      }

      val countInDb = suggestionDao.count()
      Timber.i("Tag metadata sync completed. Total values in DB: $countInDb")
      Result.success(countInDb)
    } catch (e: Exception) {
      Timber.e(e, "Tag metadata sync failed")
      Result.failure(e)
    }
  }

  override fun getCachedTagCountFlow(): kotlinx.coroutines.flow.Flow<Int> =
    suggestionDao.countFlow()

  override suspend fun getCachedTagCount(): Int = withContext(dispatchers.io) {
    suggestionDao.count()
  }
}
