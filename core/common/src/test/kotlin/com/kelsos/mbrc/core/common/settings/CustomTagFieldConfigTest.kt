package com.kelsos.mbrc.core.common.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CustomTagFieldConfigTest {

  @Test
  fun `default tags contain Energy Genre and Instruments`() {
    val defaults = CustomTagFieldConfig.DEFAULT_TAGS
    assertThat(defaults).hasSize(3)
    assertThat(defaults[0]).isEqualTo(CustomTagFieldConfig("Energy", isMultiValue = false, isEnabled = true))
    assertThat(defaults[1]).isEqualTo(CustomTagFieldConfig("Genre", isMultiValue = true, isEnabled = true))
    assertThat(defaults[2]).isEqualTo(CustomTagFieldConfig("Instruments", isMultiValue = true, isEnabled = true))
  }

  @Test
  fun `encodeList and decodeList round-trip preserves list`() {
    val original = listOf(
      CustomTagFieldConfig("Energy", isMultiValue = false, isEnabled = true),
      CustomTagFieldConfig("Genre", isMultiValue = true, isEnabled = false),
      CustomTagFieldConfig("Custom 1 / Name", isMultiValue = false, isEnabled = true)
    )

    val encoded = CustomTagFieldConfig.encodeList(original)
    val decoded = CustomTagFieldConfig.decodeList(encoded)

    assertThat(decoded).isEqualTo(original)
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
