package com.kelsos.mbrc.core.networking.protocol.payloads

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagChangeRequest(
  @Json(name = "tag")
  val tag: String,
  @Json(name = "value")
  val value: String
)
