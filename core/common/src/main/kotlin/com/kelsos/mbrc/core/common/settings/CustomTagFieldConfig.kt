package com.kelsos.mbrc.core.common.settings

import androidx.compose.runtime.Immutable
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Immutable
data class CustomTagFieldConfig(
  val tag: String,
  val isMultiValue: Boolean = false,
  val isEnabled: Boolean = true
) {
  companion object {
    val DEFAULT_TAGS = listOf(
      CustomTagFieldConfig(tag = "Energy", isMultiValue = false, isEnabled = true),
      CustomTagFieldConfig(tag = "Genre", isMultiValue = true, isEnabled = true),
      CustomTagFieldConfig(tag = "Instruments", isMultiValue = true, isEnabled = true)
    )

    fun encodeList(list: List<CustomTagFieldConfig>): String {
      if (list.isEmpty()) return "__none__"
      return list.joinToString(",") { config ->
        val encodedTag = URLEncoder.encode(config.tag, StandardCharsets.UTF_8.name())
        "$encodedTag:${config.isMultiValue}:${config.isEnabled}"
      }
    }

    fun decodeList(encoded: String): List<CustomTagFieldConfig> {
      if (encoded.isBlank() || encoded == "__none__") return emptyList()
      return try {
        encoded.split(",")
          .mapNotNull { item ->
            val parts = item.split(":")
            if (parts.size >= 3) {
              val tag = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name())
              val isMulti = parts[1].toBoolean()
              val isEnabled = parts[2].toBoolean()
              CustomTagFieldConfig(tag = tag, isMultiValue = isMulti, isEnabled = isEnabled)
            } else null
          }
      } catch (_: Exception) {
        DEFAULT_TAGS
      }
    }
  }
}
