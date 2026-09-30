package com.kelsos.mbrc.core.common.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CustomTagFieldConfigTest {

  @Test
  fun `default tags contain Energy Genre and Instruments`() {
    val defaults = CustomTagFieldConfig.DEFAULT_TAGS
    assertThat(defaults).hasSize(3)
    assertThat(defaults[0]).isEqualTo(
      CustomTagFieldConfig(
        tag = "Energy",
        isMultiValue = false,
        isEnabled = true,
        isLocked = false,
        displayType = TagDisplayType.DISCRETE_BUTTONS,
        numericScale = NumericScaleConfig(min = 1, max = 10, step = 1)
      )
    )
    assertThat(defaults[1]).isEqualTo(
      CustomTagFieldConfig(
        tag = "Genre",
        isMultiValue = true,
        isEnabled = true,
        isLocked = false,
        displayType = TagDisplayType.MULTI_CHIPS
      )
    )
    assertThat(defaults[2]).isEqualTo(
      CustomTagFieldConfig(
        tag = "Instruments",
        isMultiValue = true,
        isEnabled = true,
        isLocked = false,
        displayType = TagDisplayType.MULTI_CHIPS
      )
    )
  }

  @Test
  fun `encodeList and decodeList round-trip preserves list with new modalities`() {
    val original = listOf(
      CustomTagFieldConfig(
        tag = "Energy",
        isMultiValue = false,
        isEnabled = true,
        isLocked = true,
        displayType = TagDisplayType.DISCRETE_BUTTONS,
        numericScale = NumericScaleConfig(min = 1, max = 5, step = 1)
      ),
      CustomTagFieldConfig(
        tag = "Genre",
        isMultiValue = true,
        isEnabled = false,
        isLocked = false,
        displayType = TagDisplayType.MULTI_CHIPS
      ),
      CustomTagFieldConfig(
        tag = "Custom 1 / Name",
        isMultiValue = false,
        isEnabled = true,
        isLocked = false,
        displayType = TagDisplayType.SLIDER,
        numericScale = NumericScaleConfig(min = 0, max = 100, step = 5)
      ),
      CustomTagFieldConfig(
        tag = "Buffer Size",
        isMultiValue = false,
        isEnabled = true,
        isLocked = false,
        displayType = TagDisplayType.DISCRETE_BUTTONS,
        numericScale = NumericScaleConfig(customValues = listOf("16", "32", "64", "128"))
      )
    )

    val encoded = CustomTagFieldConfig.encodeList(original)
    val decoded = CustomTagFieldConfig.decodeList(encoded)

    assertThat(decoded).isEqualTo(original)
  }

  @Test
  fun `decodeList handles legacy 3-part encoding format seamlessly`() {
    val legacyEncoded = "Energy:false:true,Genre:true:true,Mood:true:false"
    val decoded = CustomTagFieldConfig.decodeList(legacyEncoded)

    assertThat(decoded).hasSize(3)
    assertThat(decoded[0].tag).isEqualTo("Energy")
    assertThat(decoded[0].isMultiValue).isFalse()
    assertThat(decoded[0].isEnabled).isTrue()
    assertThat(decoded[0].isLocked).isFalse()
    assertThat(decoded[0].displayType).isEqualTo(TagDisplayType.DISCRETE_BUTTONS)

    assertThat(decoded[1].tag).isEqualTo("Genre")
    assertThat(decoded[1].isMultiValue).isTrue()
    assertThat(decoded[1].isEnabled).isTrue()
    assertThat(decoded[1].displayType).isEqualTo(TagDisplayType.MULTI_CHIPS)

    assertThat(decoded[2].tag).isEqualTo("Mood")
    assertThat(decoded[2].isMultiValue).isTrue()
    assertThat(decoded[2].isEnabled).isFalse()
  }

  @Test
  fun `encodeList on empty list returns none marker and decodes to empty list`() {
    val encoded = CustomTagFieldConfig.encodeList(emptyList())
    assertThat(encoded).isEqualTo("__none__")
    assertThat(CustomTagFieldConfig.decodeList(encoded)).isEmpty()
    assertThat(CustomTagFieldConfig.decodeList("")).isEmpty()
  }

  @Test
  fun `decodeList handles malformed input gracefully`() {
    val decoded = CustomTagFieldConfig.decodeList("invalid:data")
    assertThat(decoded).isEmpty()
  }
}
