package com.kelsos.mbrc.feature.settings.domain

import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.settings.CustomTagFieldConfig
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.data.tags.CustomTagSuggestionDao
import com.kelsos.mbrc.core.data.tags.CustomTagSuggestionEntity
import com.kelsos.mbrc.core.networking.api.LibraryApi
import com.kelsos.mbrc.core.networking.dto.BrowseTagValuesResponse
import com.kelsos.mbrc.core.networking.dto.TagValuesEntryDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagMetadataSyncUseCaseImplTest {

  private val libraryApi: LibraryApi = mockk()
  private val suggestionDao: CustomTagSuggestionDao = mockk(relaxed = true)
  private val settingsManager: SettingsManager = mockk()
  private val testDispatcher = StandardTestDispatcher()
  private val dispatchers = object : AppCoroutineDispatchers {
    override val main = testDispatcher
    override val io = testDispatcher
    override val database = testDispatcher
    override val network = testDispatcher
  }

  private lateinit var useCase: TagMetadataSyncUseCaseImpl

  @Before
  fun setUp() {
    useCase = TagMetadataSyncUseCaseImpl(
      libraryApi = libraryApi,
      suggestionDao = suggestionDao,
      settingsManager = settingsManager,
      dispatchers = dispatchers
    )
  }

  @Test
  fun `syncTags queries MusicBee and stores tag values in SQLite`() = runTest(testDispatcher) {
    val configuredTags = listOf(
      CustomTagFieldConfig(tag = "Mood", isMultiValue = true, isEnabled = true),
      CustomTagFieldConfig(tag = "Energy", isMultiValue = false, isEnabled = false)
    )
    every { settingsManager.customTagFieldsFlow } returns flowOf(configuredTags)

    coEvery { libraryApi.browseTagValues(any()) } returns BrowseTagValuesResponse(
      results = listOf(
        TagValuesEntryDto(tag = "Mood", values = listOf("Energetic", "Calm")),
        TagValuesEntryDto(tag = "genre", values = listOf("Rock", "Jazz", "Electronic"))
      )
    )

    val result = useCase.syncTags()

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrNull()).isEqualTo(5)

    val insertedSlot = mutableListOf<List<CustomTagSuggestionEntity>>()
    verify { suggestionDao.insertAll(capture(insertedSlot)) }
    assertThat(insertedSlot.flatten().map { it.value }).containsExactly("Energetic", "Calm", "Rock", "Jazz", "Electronic")
    verify { suggestionDao.removeOldEntriesForTag("Mood", any()) }
    verify { suggestionDao.removeOldEntriesForTag("genre", any()) }
  }

  @Test
  fun `syncTags returns failure when libraryApi throws`() = runTest(testDispatcher) {
    every { settingsManager.customTagFieldsFlow } returns flowOf(
      listOf(CustomTagFieldConfig(tag = "Mood", isMultiValue = true, isEnabled = true))
    )

    coEvery { libraryApi.browseTagValues(any()) } throws RuntimeException("Network error")

    val result = useCase.syncTags()

    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()?.message).isEqualTo("Network error")
  }
}
