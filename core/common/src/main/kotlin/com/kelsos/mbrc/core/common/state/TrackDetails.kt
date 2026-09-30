package com.kelsos.mbrc.core.common.state

import androidx.compose.runtime.Stable

/**
 * Extended track details beyond the basic [TrackInfo].
 * Contains metadata and file properties from the currently playing track.
 */
@Stable
data class TrackDetails(
  // Tag metadata
  val albumArtist: String = "",
  val genre: String = "",
  val trackNo: String = "",
  val trackCount: String = "",
  val discNo: String = "",
  val discCount: String = "",
  val grouping: String = "",
  val publisher: String = "",
  val ratingAlbum: String = "",
  val composer: String = "",
  val comment: String = "",
  val encoder: String = "",
  val mood: String = "",

  // File properties
  val kind: String = "",
  val format: String = "",
  val size: String = "",
  val channels: String = "",
  val sampleRate: String = "",
  val bitrate: String = "",
  val dateModified: String = "",
  val dateAdded: String = "",
  val lastPlayed: String = "",
  val playCount: String = "",
  val skipCount: String = "",
  val duration: String = "",

  // Custom metadata tags (Custom1..Custom16)
  val custom1: String = "", val custom1Name: String = "",
  val custom2: String = "", val custom2Name: String = "",
  val custom3: String = "", val custom3Name: String = "",
  val custom4: String = "", val custom4Name: String = "",
  val custom5: String = "", val custom5Name: String = "",
  val custom6: String = "", val custom6Name: String = "",
  val custom7: String = "", val custom7Name: String = "",
  val custom8: String = "", val custom8Name: String = "",
  val custom9: String = "", val custom9Name: String = "",
  val custom10: String = "", val custom10Name: String = "",
  val custom11: String = "", val custom11Name: String = "",
  val custom12: String = "", val custom12Name: String = "",
  val custom13: String = "", val custom13Name: String = "",
  val custom14: String = "", val custom14Name: String = "",
  val custom15: String = "", val custom15Name: String = "",
  val custom16: String = "", val custom16Name: String = "",
  val dynamicTags: Map<String, String> = emptyMap()
) {
  companion object {
    val EMPTY = TrackDetails()
  }

  /**
   * Returns all configured custom tags that have either a defined name or non-empty value.
   */
  fun getAllCustomTags(): List<CustomTagEntry> {
    val list = mutableListOf<CustomTagEntry>()
    val pairs = listOf(
      Triple("Custom1", custom1Name, custom1),
      Triple("Custom2", custom2Name, custom2),
      Triple("Custom3", custom3Name, custom3),
      Triple("Custom4", custom4Name, custom4),
      Triple("Custom5", custom5Name, custom5),
      Triple("Custom6", custom6Name, custom6),
      Triple("Custom7", custom7Name, custom7),
      Triple("Custom8", custom8Name, custom8),
      Triple("Custom9", custom9Name, custom9),
      Triple("Custom10", custom10Name, custom10),
      Triple("Custom11", custom11Name, custom11),
      Triple("Custom12", custom12Name, custom12),
      Triple("Custom13", custom13Name, custom13),
      Triple("Custom14", custom14Name, custom14),
      Triple("Custom15", custom15Name, custom15),
      Triple("Custom16", custom16Name, custom16)
    )
    for ((slot, name, value) in pairs) {
      if (name.isNotBlank() || value.isNotBlank()) {
        val displayName = if (name.isNotBlank()) name else slot
        list.add(CustomTagEntry(slot = slot, name = displayName, value = value))
      }
    }
    val knownSlots = pairs.map { it.first.lowercase() }.toSet()
    val knownNames = pairs.map { it.second.lowercase() }.filter { it.isNotBlank() }.toSet()
    for ((tagName, value) in dynamicTags) {
      if (tagName.lowercase() !in knownSlots && tagName.lowercase() !in knownNames) {
        list.add(CustomTagEntry(slot = tagName, name = tagName, value = value))
      }
    }
    return list
  }

  /**
   * Retrieves the current value of a tag by friendly name (e.g. "Genre", "Energy", "Instruments")
   * or by slot name ("Custom1", "Custom2").
   */
  fun getTagValue(tagName: String): String {
    if (tagName.equals("genre", ignoreCase = true) || tagName.equals("genres", ignoreCase = true)) return genre
    if (tagName.equals("albumartist", ignoreCase = true)) return albumArtist
    if (tagName.equals("composer", ignoreCase = true)) return composer
    if (tagName.equals("comment", ignoreCase = true)) return comment
    if (tagName.equals("grouping", ignoreCase = true) || tagName.equals("groupings", ignoreCase = true)) return grouping
    if (tagName.equals("publisher", ignoreCase = true) || tagName.equals("publishers", ignoreCase = true)) return publisher
    if (tagName.equals("mood", ignoreCase = true) || tagName.equals("moods", ignoreCase = true)) return mood

    val clean = tagName.trim().lowercase()
    val cleanSingular = clean.trimEnd('s')

    for (entry in getAllCustomTags()) {
      val entryName = entry.name.trim().lowercase()
      val entrySlot = entry.slot.trim().lowercase()
      if (entryName == clean || (cleanSingular.isNotEmpty() && entryName.trimEnd('s') == cleanSingular) || entrySlot == clean) {
        return entry.value
      }
    }

    for ((k, v) in dynamicTags) {
      val kClean = k.trim().lowercase()
      if (kClean == clean || (cleanSingular.isNotEmpty() && kClean.trimEnd('s') == cleanSingular)) {
        return v
      }
    }

    return ""
  }

  /**
   * Returns a copy of [TrackDetails] with the specified tag updated to [value].
   * Used for instant optimistic updates before the server syncs.
   */
  fun withTagValue(tagName: String, value: String): TrackDetails {
    if (tagName.equals("genre", ignoreCase = true) || tagName.equals("genres", ignoreCase = true)) return copy(genre = value)
    if (tagName.equals("albumartist", ignoreCase = true)) return copy(albumArtist = value)
    if (tagName.equals("composer", ignoreCase = true)) return copy(composer = value)
    if (tagName.equals("comment", ignoreCase = true)) return copy(comment = value)
    if (tagName.equals("grouping", ignoreCase = true) || tagName.equals("groupings", ignoreCase = true)) return copy(grouping = value)
    if (tagName.equals("publisher", ignoreCase = true) || tagName.equals("publishers", ignoreCase = true)) return copy(publisher = value)
    if (tagName.equals("mood", ignoreCase = true) || tagName.equals("moods", ignoreCase = true)) return copy(mood = value)

    val clean = tagName.trim().lowercase()
    val cleanSingular = clean.trimEnd('s')

    fun matches(slotName: String, slotKey: String): Boolean {
      val name = slotName.trim().lowercase()
      return name == clean || (cleanSingular.isNotEmpty() && name.trimEnd('s') == cleanSingular) || slotKey.lowercase() == clean
    }

    if (matches(custom1Name, "custom1")) return copy(custom1 = value)
    if (matches(custom2Name, "custom2")) return copy(custom2 = value)
    if (matches(custom3Name, "custom3")) return copy(custom3 = value)
    if (matches(custom4Name, "custom4")) return copy(custom4 = value)
    if (matches(custom5Name, "custom5")) return copy(custom5 = value)
    if (matches(custom6Name, "custom6")) return copy(custom6 = value)
    if (matches(custom7Name, "custom7")) return copy(custom7 = value)
    if (matches(custom8Name, "custom8")) return copy(custom8 = value)
    if (matches(custom9Name, "custom9")) return copy(custom9 = value)
    if (matches(custom10Name, "custom10")) return copy(custom10 = value)
    if (matches(custom11Name, "custom11")) return copy(custom11 = value)
    if (matches(custom12Name, "custom12")) return copy(custom12 = value)
    if (matches(custom13Name, "custom13")) return copy(custom13 = value)
    if (matches(custom14Name, "custom14")) return copy(custom14 = value)
    if (matches(custom15Name, "custom15")) return copy(custom15 = value)
    if (matches(custom16Name, "custom16")) return copy(custom16 = value)

    // Dynamic fallback: store in dynamicTags dictionary without corrupting custom slots
    val updatedDynamic = dynamicTags.toMutableMap()
    val matchedKey = updatedDynamic.keys.firstOrNull { key ->
      val kClean = key.trim().lowercase()
      kClean == clean || (cleanSingular.isNotEmpty() && kClean.trimEnd('s') == cleanSingular)
    } ?: tagName.trim()
    updatedDynamic[matchedKey] = value
    return copy(dynamicTags = updatedDynamic)
  }

  /**
   * Returns true if this contains any meaningful data.
   */
  fun hasData(): Boolean = this != EMPTY

  /**
   * Formats the track/disc number as "track/total" or just "track" if no total.
   */
  fun formatTrackNumber(): String = when {
    trackNo.isBlank() -> ""
    trackCount.isBlank() -> trackNo
    else -> "$trackNo/$trackCount"
  }

  /**
   * Formats the disc number as "disc/total" or just "disc" if no total.
   */
  fun formatDiscNumber(): String = when {
    discNo.isBlank() -> ""
    discCount.isBlank() -> discNo
    else -> "$discNo/$discCount"
  }
}

/**
 * Representation of a custom metadata tag entry.
 */
@Stable
data class CustomTagEntry(
  val slot: String,
  val name: String,
  val value: String
)
