package com.kelsos.mbrc.core.data.tags

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CustomTagSuggestionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertAll(list: List<CustomTagSuggestionEntity>)

  @Query("SELECT value FROM custom_tag_suggestions WHERE LOWER(tag) = LOWER(:tag) ORDER BY value COLLATE NOCASE ASC")
  fun getSuggestionsForTag(tag: String): List<String>

  @Query("SELECT DISTINCT tag FROM custom_tag_suggestions")
  fun getAllTags(): List<String>

  @Query("SELECT value FROM custom_tag_suggestions WHERE LOWER(tag) = LOWER(:tag) AND LOWER(value) = LOWER(:value) LIMIT 1")
  fun findTagValue(tag: String, value: String): String?

  @Query("SELECT DISTINCT LOWER(value) FROM custom_tag_suggestions WHERE LOWER(tag) = LOWER(:tag)")
  fun getConfirmedValuesForTag(tag: String): List<String>

  @Query("SELECT DISTINCT LOWER(tag || ':' || value) FROM custom_tag_suggestions")
  fun getAllConfirmedTagValues(): List<String>

  @Query("SELECT DISTINCT LOWER(tag || ':' || value) FROM custom_tag_suggestions")
  fun getAllConfirmedTagValuesFlow(): kotlinx.coroutines.flow.Flow<List<String>>

  @Query("DELETE FROM custom_tag_suggestions WHERE LOWER(tag) = LOWER(:tag) AND date_added < :timestamp")
  fun removeOldEntriesForTag(tag: String, timestamp: Long)

  @Query("DELETE FROM custom_tag_suggestions WHERE LOWER(tag) = LOWER(:tag)")
  fun deleteByTag(tag: String)

  @Query("DELETE FROM custom_tag_suggestions")
  fun clearAll()
}
