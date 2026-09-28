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
  val custom16: String = "", val custom16Name: String = ""
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
    return list
  }

  /**
   * Retrieves the current value of a tag by friendly name (e.g. "Genre", "Energy", "Instruments")
   * or by slot name ("Custom1", "Custom2").
   */
  fun getTagValue(tagName: String): String {
    if (tagName.equals("genre", ignoreCase = true)) return genre
    if (tagName.equals("albumartist", ignoreCase = true)) return albumArtist
    if (tagName.equals("composer", ignoreCase = true)) return composer
    if (tagName.equals("comment", ignoreCase = true)) return comment
    if (tagName.equals("grouping", ignoreCase = true)) return grouping
    if (tagName.equals("publisher", ignoreCase = true)) return publisher

    for (entry in getAllCustomTags()) {
      if (entry.name.equals(tagName, ignoreCase = true) || entry.slot.equals(tagName, ignoreCase = true)) {
        return entry.value
      }
    }
    return ""
  }

  /**
   * Returns a copy of [TrackDetails] with the specified tag updated to [value].
   * Used for instant optimistic updates before the server syncs.
   */
  fun withTagValue(tagName: String, value: String): TrackDetails {
    if (tagName.equals("genre", ignoreCase = true)) return copy(genre = value)
    if (tagName.equals("albumartist", ignoreCase = true)) return copy(albumArtist = value)
    if (tagName.equals("composer", ignoreCase = true)) return copy(composer = value)
    if (tagName.equals("comment", ignoreCase = true)) return copy(comment = value)
    if (tagName.equals("grouping", ignoreCase = true)) return copy(grouping = value)
    if (tagName.equals("publisher", ignoreCase = true)) return copy(publisher = value)

    if (custom1Name.equals(tagName, true) || tagName.equals("custom1", true)) return copy(custom1 = value)
    if (custom2Name.equals(tagName, true) || tagName.equals("custom2", true)) return copy(custom2 = value)
    if (custom3Name.equals(tagName, true) || tagName.equals("custom3", true)) return copy(custom3 = value)
    if (custom4Name.equals(tagName, true) || tagName.equals("custom4", true)) return copy(custom4 = value)
    if (custom5Name.equals(tagName, true) || tagName.equals("custom5", true)) return copy(custom5 = value)
    if (custom6Name.equals(tagName, true) || tagName.equals("custom6", true)) return copy(custom6 = value)
    if (custom7Name.equals(tagName, true) || tagName.equals("custom7", true)) return copy(custom7 = value)
    if (custom8Name.equals(tagName, true) || tagName.equals("custom8", true)) return copy(custom8 = value)
    if (custom9Name.equals(tagName, true) || tagName.equals("custom9", true)) return copy(custom9 = value)
    if (custom10Name.equals(tagName, true) || tagName.equals("custom10", true)) return copy(custom10 = value)
    if (custom11Name.equals(tagName, true) || tagName.equals("custom11", true)) return copy(custom11 = value)
    if (custom12Name.equals(tagName, true) || tagName.equals("custom12", true)) return copy(custom12 = value)
    if (custom13Name.equals(tagName, true) || tagName.equals("custom13", true)) return copy(custom13 = value)
    if (custom14Name.equals(tagName, true) || tagName.equals("custom14", true)) return copy(custom14 = value)
    if (custom15Name.equals(tagName, true) || tagName.equals("custom15", true)) return copy(custom15 = value)
    if (custom16Name.equals(tagName, true) || tagName.equals("custom16", true)) return copy(custom16 = value)

    // Fallback: assign to custom1 if empty or unmatched
    return copy(custom1 = value, custom1Name = if (custom1Name.isBlank()) tagName else custom1Name)
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
