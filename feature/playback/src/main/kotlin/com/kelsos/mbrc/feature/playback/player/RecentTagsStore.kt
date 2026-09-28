package com.kelsos.mbrc.feature.playback.player

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

interface RecentTagsStore {
  fun getRecentTags(tag: String): List<String>
  fun getRecentTagsFlow(tag: String): Flow<List<String>>
  suspend fun recordTagUsed(tag: String, value: String)
}

class RecentTagsStoreImpl(
  context: Context,
  private val moshi: Moshi = Moshi.Builder().build()
) : RecentTagsStore {

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val listType = Types.newParameterizedType(List::class.java, String::class.java)
  private val adapter = moshi.adapter<List<String>>(listType)

  private val flows = ConcurrentHashMap<String, MutableStateFlow<List<String>>>()

  override fun getRecentTags(tag: String): List<String> {
    val key = keyFor(tag)
    val json = prefs.getString(key, null) ?: return emptyList()
    return try {
      adapter.fromJson(json)?.filter { it.isNotBlank() } ?: emptyList()
    } catch (_: Exception) {
      emptyList()
    }
  }

  override fun getRecentTagsFlow(tag: String): Flow<List<String>> {
    val normalized = tag.trim().lowercase()
    return flows.computeIfAbsent(normalized) {
      MutableStateFlow(getRecentTags(normalized))
    }.asStateFlow()
  }

  override suspend fun recordTagUsed(tag: String, value: String) {
    val trimmedValue = value.trim()
    if (trimmedValue.isBlank()) return
    val normalizedTag = tag.trim().lowercase()
    val current = getRecentTags(normalizedTag).toMutableList()

    // Move to front (LRU) and deduplicate case-insensitively
    current.removeAll { it.equals(trimmedValue, ignoreCase = true) }
    current.add(0, trimmedValue)

    val capped = current.take(MAX_RECENT_TAGS)
    val json = adapter.toJson(capped)
    prefs.edit().putString(keyFor(normalizedTag), json).apply()

    flows[normalizedTag]?.value = capped
  }

  private fun keyFor(tag: String): String = "recent_tags_${tag.trim().lowercase()}"

  companion object {
    private const val PREFS_NAME = "mbrc_recent_tags"
    private const val MAX_RECENT_TAGS = 30
  }
}
