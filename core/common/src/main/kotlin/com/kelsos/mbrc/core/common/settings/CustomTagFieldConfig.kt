package com.kelsos.mbrc.core.common.settings

import androidx.compose.runtime.Immutable
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class TagDisplayType {
  MULTI_CHIPS,
  DISCRETE_BUTTONS,
  SLIDER,
  TEXT
}

@Immutable
data class NumericScaleConfig(
  val min: Int = 0,
  val max: Int = 10,
  val step: Int = 1,
  val customValues: List<String> = emptyList()
) {
  fun getResolvedValues(): List<String> {
    if (customValues.isNotEmpty()) return customValues
    if (min <= max && step > 0) {
      val list = mutableListOf<String>()
      var curr = min
      while (curr <= max) {
        list.add(curr.toString())
        curr += step
      }
      return list
    }
    return (0..10).map { it.toString() }
  }
}

@Immutable
data class CustomTagFieldConfig(
  val tag: String,
  val isMultiValue: Boolean = false,
  val isEnabled: Boolean = true,
  val isLocked: Boolean = false,
  val displayType: TagDisplayType = if (isMultiValue) TagDisplayType.MULTI_CHIPS else TagDisplayType.DISCRETE_BUTTONS,
  val numericScale: NumericScaleConfig = NumericScaleConfig()
) {
  val isMulti: Boolean
    get() = displayType == TagDisplayType.MULTI_CHIPS || isMultiValue

  companion object {
    val DEFAULT_TAGS = listOf(
      CustomTagFieldConfig(
        tag = "Energy",
        isMultiValue = false,
        isEnabled = true,
        isLocked = false,
        displayType = TagDisplayType.DISCRETE_BUTTONS,
        numericScale = NumericScaleConfig(min = 1, max = 10, step = 1)
      ),
      CustomTagFieldConfig(
        tag = "Genre",
        isMultiValue = true,
        isEnabled = true,
        isLocked = false,
        displayType = TagDisplayType.MULTI_CHIPS
      ),
      CustomTagFieldConfig(
        tag = "Instruments",
        isMultiValue = true,
        isEnabled = true,
        isLocked = false,
        displayType = TagDisplayType.MULTI_CHIPS
      )
    )

    fun encodeList(list: List<CustomTagFieldConfig>): String {
      if (list.isEmpty()) return "__none__"
      return list.joinToString(",") { config ->
        val encodedTag = URLEncoder.encode(config.tag, StandardCharsets.UTF_8.name())
        val customValuesStr = config.numericScale.customValues.joinToString("~")
        "$encodedTag:${config.isMultiValue}:${config.isEnabled}:${config.isLocked}:${config.displayType.name}:${config.numericScale.min}:${config.numericScale.max}:${config.numericScale.step}:$customValuesStr"
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
              val isLocked = if (parts.size >= 4) parts[3].toBoolean() else false
              val displayType = if (parts.size >= 5) {
                runCatching { TagDisplayType.valueOf(parts[4]) }
                  .getOrDefault(if (isMulti) TagDisplayType.MULTI_CHIPS else TagDisplayType.DISCRETE_BUTTONS)
              } else {
                if (isMulti) TagDisplayType.MULTI_CHIPS else TagDisplayType.DISCRETE_BUTTONS
              }
              val scale = if (parts.size >= 9) {
                val min = parts[5].toIntOrNull() ?: 0
                val max = parts[6].toIntOrNull() ?: 10
                val step = parts[7].toIntOrNull() ?: 1
                val custom = if (parts[8].isNotBlank()) parts[8].split("~") else emptyList()
                NumericScaleConfig(min = min, max = max, step = step, customValues = custom)
              } else {
                NumericScaleConfig()
              }
              CustomTagFieldConfig(
                tag = tag,
                isMultiValue = (displayType == TagDisplayType.MULTI_CHIPS),
                isEnabled = isEnabled,
                isLocked = isLocked,
                displayType = displayType,
                numericScale = scale
              )
            } else null
          }
      } catch (_: Exception) {
        DEFAULT_TAGS
      }
    }
  }
}
