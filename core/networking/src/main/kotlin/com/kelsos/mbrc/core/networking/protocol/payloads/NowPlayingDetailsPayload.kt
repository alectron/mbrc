package com.kelsos.mbrc.core.networking.protocol.payloads

import com.kelsos.mbrc.core.common.state.TrackDetails
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Payload for the nowplayingdetails protocol message.
 * Contains extended track metadata and file properties.
 */
@JsonClass(generateAdapter = true)
data class NowPlayingDetailsPayload(
  // Tag metadata
  @Json(name = "albumArtist")
  val albumArtist: String = "",
  @Json(name = "genre")
  val genre: String = "",
  @Json(name = "trackNo")
  val trackNo: String = "",
  @Json(name = "trackCount")
  val trackCount: String = "",
  @Json(name = "discNo")
  val discNo: String = "",
  @Json(name = "discCount")
  val discCount: String = "",
  @Json(name = "grouping")
  val grouping: String = "",
  @Json(name = "publisher")
  val publisher: String = "",
  @Json(name = "ratingAlbum")
  val ratingAlbum: String = "",
  @Json(name = "composer")
  val composer: String = "",
  @Json(name = "comment")
  val comment: String = "",
  @Json(name = "encoder")
  val encoder: String = "",
  @Json(name = "mood")
  val mood: String = "",

  // File properties
  @Json(name = "kind")
  val kind: String = "",
  @Json(name = "format")
  val format: String = "",
  @Json(name = "size")
  val size: String = "",
  @Json(name = "channels")
  val channels: String = "",
  @Json(name = "sampleRate")
  val sampleRate: String = "",
  @Json(name = "bitrate")
  val bitrate: String = "",
  @Json(name = "dateModified")
  val dateModified: String = "",
  @Json(name = "dateAdded")
  val dateAdded: String = "",
  @Json(name = "lastPlayed")
  val lastPlayed: String = "",
  @Json(name = "playCount")
  val playCount: String = "",
  @Json(name = "skipCount")
  val skipCount: String = "",
  @Json(name = "duration")
  val duration: String = "",

  @Json(name = "custom1") val custom1: String = "",
  @Json(name = "custom1Name") val custom1Name: String = "",
  @Json(name = "custom2") val custom2: String = "",
  @Json(name = "custom2Name") val custom2Name: String = "",
  @Json(name = "custom3") val custom3: String = "",
  @Json(name = "custom3Name") val custom3Name: String = "",
  @Json(name = "custom4") val custom4: String = "",
  @Json(name = "custom4Name") val custom4Name: String = "",
  @Json(name = "custom5") val custom5: String = "",
  @Json(name = "custom5Name") val custom5Name: String = "",
  @Json(name = "custom6") val custom6: String = "",
  @Json(name = "custom6Name") val custom6Name: String = "",
  @Json(name = "custom7") val custom7: String = "",
  @Json(name = "custom7Name") val custom7Name: String = "",
  @Json(name = "custom8") val custom8: String = "",
  @Json(name = "custom8Name") val custom8Name: String = "",
  @Json(name = "custom9") val custom9: String = "",
  @Json(name = "custom9Name") val custom9Name: String = "",
  @Json(name = "custom10") val custom10: String = "",
  @Json(name = "custom10Name") val custom10Name: String = "",
  @Json(name = "custom11") val custom11: String = "",
  @Json(name = "custom11Name") val custom11Name: String = "",
  @Json(name = "custom12") val custom12: String = "",
  @Json(name = "custom12Name") val custom12Name: String = "",
  @Json(name = "custom13") val custom13: String = "",
  @Json(name = "custom13Name") val custom13Name: String = "",
  @Json(name = "custom14") val custom14: String = "",
  @Json(name = "custom14Name") val custom14Name: String = "",
  @Json(name = "custom15") val custom15: String = "",
  @Json(name = "custom15Name") val custom15Name: String = "",
  @Json(name = "custom16") val custom16: String = "",
  @Json(name = "custom16Name") val custom16Name: String = ""
) {
  /**
   * Converts this payload to a [TrackDetails] domain model.
   */
  fun toTrackDetails(): TrackDetails = TrackDetails(
    albumArtist = albumArtist,
    genre = genre,
    trackNo = trackNo,
    trackCount = trackCount,
    discNo = discNo,
    discCount = discCount,
    grouping = grouping,
    publisher = publisher,
    ratingAlbum = ratingAlbum,
    composer = composer,
    comment = comment,
    encoder = encoder,
    mood = mood,
    kind = kind,
    format = format,
    size = size,
    channels = channels,
    sampleRate = sampleRate,
    bitrate = bitrate,
    dateModified = dateModified,
    dateAdded = dateAdded,
    lastPlayed = lastPlayed,
    playCount = playCount,
    skipCount = skipCount,
    duration = duration,
    custom1 = custom1, custom1Name = custom1Name,
    custom2 = custom2, custom2Name = custom2Name,
    custom3 = custom3, custom3Name = custom3Name,
    custom4 = custom4, custom4Name = custom4Name,
    custom5 = custom5, custom5Name = custom5Name,
    custom6 = custom6, custom6Name = custom6Name,
    custom7 = custom7, custom7Name = custom7Name,
    custom8 = custom8, custom8Name = custom8Name,
    custom9 = custom9, custom9Name = custom9Name,
    custom10 = custom10, custom10Name = custom10Name,
    custom11 = custom11, custom11Name = custom11Name,
    custom12 = custom12, custom12Name = custom12Name,
    custom13 = custom13, custom13Name = custom13Name,
    custom14 = custom14, custom14Name = custom14Name,
    custom15 = custom15, custom15Name = custom15Name,
    custom16 = custom16, custom16Name = custom16Name
  )
}
