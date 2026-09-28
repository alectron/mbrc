package com.kelsos.mbrc.feature.playback.player.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelsos.mbrc.core.common.settings.CustomTagFieldConfig
import com.kelsos.mbrc.feature.playback.R
import com.kelsos.mbrc.feature.playback.player.RatingDialogViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBottomSheet(
  isScrobbling: Boolean,
  onScrobbleToggle: () -> Unit,
  onShowTrackDetails: () -> Unit,
  onGoToAlbum: (() -> Unit)?,
  onGoToArtist: (() -> Unit)?,
  onDismiss: () -> Unit,
  isBanned: Boolean,
  isStream: Boolean,
  onBanClick: () -> Unit,
  viewModel: RatingDialogViewModel = koinViewModel()
) {
  val sheetState = rememberModalBottomSheetState()
  val rating by viewModel.rating.collectAsStateWithLifecycle(initialValue = null)
  val halfStarEnabled by viewModel.halfStarEnabled.collectAsStateWithLifecycle()
  val customTagFields by viewModel.customTagFields.collectAsStateWithLifecycle()
  val trackDetails by viewModel.trackDetails.collectAsStateWithLifecycle()
  val genreSuggestions by viewModel.genreSuggestions.collectAsStateWithLifecycle()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp)
    ) {
      // Track Details option
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = {
            onDismiss()
            onShowTrackDetails()
          })
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = stringResource(R.string.track_details_title),
          style = MaterialTheme.typography.bodyLarge
        )
      }

      // Go to Album option
      if (onGoToAlbum != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
              onDismiss()
              onGoToAlbum()
            })
            .padding(vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Album,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = stringResource(R.string.player_go_to_album),
            style = MaterialTheme.typography.bodyLarge
          )
        }
      }

      // Go to Artist option
      if (onGoToArtist != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
              onDismiss()
              onGoToArtist()
            })
            .padding(vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = stringResource(R.string.player_go_to_artist),
            style = MaterialTheme.typography.bodyLarge
          )
        }
      }

      // Ban - grouped with scrobbling because both are Last.fm, and rare enough that it does not
      // need to compete with the track title for room on the player itself.
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(
            enabled = !isStream,
            onClick = {
              onDismiss()
              onBanClick()
            }
          )
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.ThumbDown,
          contentDescription = null,
          tint = when {
            isStream -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            isBanned -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = stringResource(R.string.player_lfm_ban),
          style = MaterialTheme.typography.bodyLarge,
          color = if (isStream) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
          } else {
            MaterialTheme.colorScheme.onSurface
          }
        )
      }

      // Scrobbling toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onScrobbleToggle)
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(R.string.lastfm_scrobble),
          style = MaterialTheme.typography.bodyLarge
        )
        Switch(
          checked = isScrobbling,
          onCheckedChange = { onScrobbleToggle() }
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Rating section
      Text(
        text = stringResource(R.string.rate_the_playing_track),
        style = MaterialTheme.typography.bodyLarge
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Rating bar with bomb, stars, and clear
      RatingBar(
        rating = rating,
        onRatingChange = { viewModel.changeRating(it) },
        halfStarEnabled = halfStarEnabled,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Custom Tag Fields section
      if (customTagFields.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
          modifier = Modifier.padding(vertical = 8.dp),
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Text(
          text = stringResource(R.string.custom_tags_section_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(vertical = 8.dp)
        )

        customTagFields.forEach { config ->
          CustomTagFieldRow(
            config = config,
            currentValue = trackDetails.getTagValue(config.tag),
            onValueChange = { newValue -> viewModel.changeTag(config.tag, newValue) },
            suggestions = if (config.tag.equals("genre", ignoreCase = true)) genreSuggestions else emptyList()
          )
          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomTagFieldRow(
  config: CustomTagFieldConfig,
  currentValue: String,
  onValueChange: (String) -> Unit,
  suggestions: List<String>
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = config.tag,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(6.dp))

    if (config.isMultiValue) {
      val values = remember(currentValue) {
        currentValue.split(";")
          .map { it.trim() }
          .filter { it.isNotEmpty() }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        values.forEach { tagItem ->
          InputChip(
            selected = true,
            onClick = { /* keep selected */ },
            label = { Text(tagItem) },
            trailingIcon = {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(R.string.custom_tags_clear),
                modifier = Modifier
                  .size(16.dp)
                  .clickable {
                    val remaining = values.filterNot { it.equals(tagItem, ignoreCase = true) }
                    onValueChange(remaining.joinToString("; "))
                  }
              )
            }
          )
        }

        AssistChip(
          onClick = { showAddDialog = true },
          label = { Text(stringResource(R.string.custom_tags_add, config.tag)) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
          }
        )
      }

      if (showAddDialog) {
        AddTagValueDialog(
          tagName = config.tag,
          suggestions = suggestions,
          onAdd = { newTag ->
            val updated = (values + newTag).distinct()
            onValueChange(updated.joinToString("; "))
            showAddDialog = false
          },
          onDismiss = { showAddDialog = false }
        )
      }
    } else {
      // Single value
      if (config.tag.equals("energy", ignoreCase = true)) {
        // Quick 1-10 selector chips for Energy
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          (1..10).forEach { num ->
            val numStr = num.toString()
            val isSelected = currentValue.trim() == numStr
            FilterChip(
              selected = isSelected,
              onClick = {
                if (isSelected) onValueChange("") else onValueChange(numStr)
              },
              label = { Text(numStr) }
            )
          }

          if (currentValue.isNotBlank()) {
            IconButton(
              onClick = { onValueChange("") },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(R.string.custom_tags_clear),
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      } else {
        // General single value: display with edit button
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showEditDialog = true }
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = currentValue.ifBlank { stringResource(R.string.custom_tags_empty) },
            style = MaterialTheme.typography.bodyMedium,
            color = if (currentValue.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
          )

          IconButton(onClick = { showEditDialog = true }) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = stringResource(R.string.custom_tags_edit_title, config.tag),
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (showEditDialog) {
          EditSingleTagValueDialog(
            tagName = config.tag,
            initialValue = currentValue,
            onSave = { newValue ->
              onValueChange(newValue)
              showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTagValueDialog(
  tagName: String,
  suggestions: List<String>,
  onAdd: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var text by remember { mutableStateOf("") }
  val filteredSuggestions = remember(text, suggestions) {
    if (text.isBlank()) suggestions.take(8)
    else suggestions.filter { it.contains(text, ignoreCase = true) }.take(8)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.custom_tags_add_title, tagName)) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text(stringResource(R.string.custom_tags_value_hint)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (filteredSuggestions.isNotEmpty()) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Suggestions:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            filteredSuggestions.forEach { suggestion ->
              SuggestionChip(
                onClick = { onAdd(suggestion) },
                label = { Text(suggestion) }
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (text.isNotBlank()) {
            onAdd(text.trim())
          }
        },
        enabled = text.isNotBlank()
      ) {
        Text(stringResource(android.R.string.ok))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(android.R.string.cancel))
      }
    }
  )
}

@Composable
private fun EditSingleTagValueDialog(
  tagName: String,
  initialValue: String,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var text by remember { mutableStateOf(initialValue) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.custom_tags_edit_title, tagName)) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text(stringResource(R.string.custom_tags_value_hint)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { onSave(text.trim()) }) {
        Text(stringResource(android.R.string.ok))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(android.R.string.cancel))
      }
    }
  )
}
