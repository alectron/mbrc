package com.kelsos.mbrc.feature.playback.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RecentTagsStoreTest {

  private lateinit var context: Context
  private lateinit var store: RecentTagsStore

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    // Clear preferences
    context.getSharedPreferences("mbrc_recent_tags", Context.MODE_PRIVATE).edit().clear().commit()
    store = RecentTagsStoreImpl(context)
  }

  @Test
  fun `getRecentTags returns empty list initially`() {
    assertThat(store.getRecentTags("mood")).isEmpty()
  }

  @Test
  fun `recordTagUsed saves and retrieves tags in LRU order`() = runTest {
    store.recordTagUsed("mood", "Energetic")
    store.recordTagUsed("mood", "Chill")
    store.recordTagUsed("mood", "Dark")

    // Dark was added last, so it should be first
    val tags = store.getRecentTags("mood")
    assertThat(tags).containsExactly("Dark", "Chill", "Energetic").inOrder()
  }

  @Test
  fun `recordTagUsed moves existing tag to front and deduplicates case-insensitively`() = runTest {
    store.recordTagUsed("genre", "Rock")
    store.recordTagUsed("genre", "Jazz")
    store.recordTagUsed("genre", "Pop")

    // Now re-record "rock" in lower case
    store.recordTagUsed("genre", "rock")

    val tags = store.getRecentTags("genre")
    assertThat(tags).containsExactly("rock", "Pop", "Jazz").inOrder()
  }

  @Test
  fun `recordTagUsed ignores blank values`() = runTest {
    store.recordTagUsed("mood", "   ")
    store.recordTagUsed("mood", "")

    assertThat(store.getRecentTags("mood")).isEmpty()
  }

  @Test
  fun `tags for different tag names are isolated`() = runTest {
    store.recordTagUsed("mood", "Happy")
    store.recordTagUsed("genre", "Electronic")

    assertThat(store.getRecentTags("mood")).containsExactly("Happy")
    assertThat(store.getRecentTags("genre")).containsExactly("Electronic")
  }
}
