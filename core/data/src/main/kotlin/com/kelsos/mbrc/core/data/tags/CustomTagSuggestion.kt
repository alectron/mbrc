package com.kelsos.mbrc.core.data.tags

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
data class CustomTagSuggestion(
  val tag: String,
  val value: String,
  val id: Long = 0
)

@Entity(
  tableName = "custom_tag_suggestions",
  indices = [
    Index(
      value = ["tag", "value"],
      name = "custom_tag_suggestions_tag_value_idx",
      unique = true
    )
  ]
)
data class CustomTagSuggestionEntity(
  @ColumnInfo(name = "tag")
  val tag: String,
  @ColumnInfo(name = "value")
  val value: String,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long = 0,
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0
) {
  fun toSuggestion(): CustomTagSuggestion = CustomTagSuggestion(tag = tag, value = value, id = id)
}
