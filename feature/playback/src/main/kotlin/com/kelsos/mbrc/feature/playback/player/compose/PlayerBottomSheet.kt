package com.kelsos.mbrc.feature.playback.player.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val rating by viewModel.rating.collectAsStateWithLifecycle(initialValue = null)
  val halfStarEnabled by viewModel.halfStarEnabled.collectAsStateWithLifecycle()
  val customTagFields by viewModel.customTagFields.collectAsStateWithLifecycle()
  val trackDetails by viewModel.trackDetails.collectAsStateWithLifecycle()
  val genreSuggestions by viewModel.genreSuggestions.collectAsStateWithLifecycle()
  val confirmedTags by viewModel.confirmedTags.collectAsStateWithLifecycle()
  val tagSuggestionLimit by viewModel.tagSuggestionLimit.collectAsStateWithLifecycle()
  val tagSuggestionsMap by viewModel.tagSuggestionsMap.collectAsStateWithLifecycle()
  val allTagValuesMap by viewModel.allTagValuesMap.collectAsStateWithLifecycle()
  val tagClipboard by viewModel.tagClipboard.collectAsStateWithLifecycle()
  val pendingReorders by viewModel.pendingReorders.collectAsStateWithLifecycle()

  val handleDismiss = {
    viewModel.commitAllPendingReorders()
    onDismiss()
  }

  DisposableEffect(Unit) {
    onDispose {
      viewModel.commitAllPendingReorders()
    }
  }

  ModalBottomSheet(
    onDismissRequest = handleDismiss,
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
            handleDismiss()
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
              handleDismiss()
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
              handleDismiss()
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
              handleDismiss()
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
          val normalizedKey = config.tag.trim().lowercase()
          val quickSuggestions = tagSuggestionsMap[normalizedKey] ?: viewModel.getSuggestionsForTag(config.tag)
          val allValues = allTagValuesMap[normalizedKey] ?: viewModel.getAllValuesForTag(config.tag)
          val hasClipboard = tagClipboard[normalizedKey] != null
          val isReordered = pendingReorders.containsKey(normalizedKey)
          CustomTagFieldRow(
            config = config,
            currentValue = trackDetails.getTagValue(config.tag),
            onValueChange = { newValue -> viewModel.changeTag(config.tag, newValue) },
            quickSuggestions = quickSuggestions,
            allValues = allValues,
            isTagConfirmed = { tag, value -> viewModel.isTagConfirmed(tag, value) },
            hasClipboard = hasClipboard,
            onCopy = { values -> viewModel.copyTags(config.tag, values) },
            onPaste = { viewModel.pasteTags(config.tag) },
            hasPendingReorder = isReordered,
            onPushReorder = { viewModel.commitPendingReorder(config.tag) },
            onStageReorder = { newValues -> viewModel.stageReorder(config.tag, newValues) }
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
  quickSuggestions: List<String>,
  allValues: List<String>,
  isTagConfirmed: (String, String) -> Boolean,
  hasClipboard: Boolean = false,
  onCopy: ((List<String>) -> Unit)? = null,
  onPaste: (() -> Unit)? = null,
  hasPendingReorder: Boolean = false,
  onPushReorder: (() -> Unit)? = null,
  onStageReorder: ((List<String>) -> Unit)? = null
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = config.tag,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )

        if (hasPendingReorder && onPushReorder != null) {
          IconButton(
            onClick = onPushReorder,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = stringResource(R.string.custom_tags_push_order, config.tag),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      if (config.isMultiValue) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (hasClipboard && onPaste != null) {
            IconButton(
              onClick = onPaste,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = stringResource(R.string.custom_tags_paste, config.tag),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          val hasValues = currentValue.split(";").any { it.trim().isNotBlank() }
          if (hasValues && onCopy != null) {
            IconButton(
              onClick = {
                val list = currentValue.split(";").map { it.trim() }.filter { it.isNotBlank() }
                onCopy(list)
              },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.custom_tags_copy, config.tag),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (config.isMultiValue) {
      var localValues by remember(currentValue) {
        mutableStateOf(
          currentValue.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        )
      }

      var draggingIndex by remember { mutableStateOf<Int?>(null) }
      var hoverDropSlot by remember { mutableStateOf<Int?>(null) }
      var dragOffset by remember { mutableStateOf(Offset.Zero) }
      var flowRowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
      val chipBounds = remember { mutableMapOf<Int, Rect>() }

      LaunchedEffect(localValues) {
        chipBounds.clear()
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .onGloballyPositioned { flowRowCoordinates = it }
      ) {
        localValues.forEachIndexed { index, tagItem ->
          val isConfirmed = isTagConfirmed(config.tag, tagItem)
          val isDragging = draggingIndex == index

          // Drop gap expanding right before this chip
          val isGapBefore = (hoverDropSlot == index && draggingIndex != null && draggingIndex != index && draggingIndex != index - 1)
          val gapWidthBefore by animateDpAsState(
            targetValue = if (isGapBefore) 52.dp else 0.dp,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            label = "gap_before_$index"
          )

          if (gapWidthBefore > 0.dp) {
            Box(
              modifier = Modifier
                .width(gapWidthBefore)
                .height(32.dp)
                .border(
                  width = 1.5.dp,
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                  shape = RoundedCornerShape(16.dp)
                )
                .background(
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(16.dp)
                )
            )
          }

          Box(
            modifier = Modifier
              .zIndex(if (isDragging) 10f else 1f)
              .graphicsLayer {
                if (isDragging) {
                  translationX = dragOffset.x
                  translationY = dragOffset.y
                  scaleX = 1.08f
                  scaleY = 1.08f
                  shadowElevation = 8.dp.toPx()
                }
              }
              .onGloballyPositioned { coords ->
                flowRowCoordinates?.let { parent ->
                  val topLeft = parent.localPositionOf(coords, Offset.Zero)
                  chipBounds[index] = Rect(
                    topLeft,
                    Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                  )
                }
              }
              .pointerInput(localValues) {
                detectDragGesturesAfterLongPress(
                  onDragStart = {
                    draggingIndex = index
                    hoverDropSlot = null
                    dragOffset = Offset.Zero
                  },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                    val currentIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                    val currentRect = chipBounds[currentIdx] ?: return@detectDragGesturesAfterLongPress
                    val pointerCenter = currentRect.center + dragOffset

                    var candidateSlot: Int? = null
                    for ((targetIdx, targetRect) in chipBounds) {
                      if (targetRect.contains(pointerCenter)) {
                        candidateSlot = if (pointerCenter.x < targetRect.center.x) {
                          targetIdx
                        } else {
                          targetIdx + 1
                        }
                        break
                      }
                    }
                    if (candidateSlot != null) {
                      hoverDropSlot = candidateSlot.coerceIn(0, localValues.size)
                    }
                  },
                  onDragEnd = {
                    val fromIdx = draggingIndex
                    val toSlot = hoverDropSlot
                    draggingIndex = null
                    hoverDropSlot = null
                    dragOffset = Offset.Zero

                    if (fromIdx != null && toSlot != null && fromIdx in 0 until localValues.size) {
                      val targetIndex = (if (toSlot > fromIdx) toSlot - 1 else toSlot).coerceIn(0, localValues.size - 1)
                      if (targetIndex != fromIdx) {
                        val reordered = localValues.toMutableList()
                        val item = reordered.removeAt(fromIdx)
                        reordered.add(targetIndex, item)
                        localValues = reordered

                        if (onStageReorder != null) {
                          onStageReorder(reordered)
                        } else {
                          onValueChange(reordered.joinToString("; "))
                        }
                      }
                    }
                  },
                  onDragCancel = {
                    draggingIndex = null
                    hoverDropSlot = null
                    dragOffset = Offset.Zero
                  }
                )
              }
          ) {
            InputChip(
              selected = true,
              onClick = { /* keep selected */ },
              label = { Text(tagItem) },
              colors = InputChipDefaults.inputChipColors(
                selectedContainerColor = if (isConfirmed) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceVariant
                },
                selectedLabelColor = if (isConfirmed) {
                  MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                }
              ),
              border = if (isConfirmed) {
                null
              } else {
                InputChipDefaults.inputChipBorder(
                  enabled = true,
                  selected = true,
                  borderColor = MaterialTheme.colorScheme.outline
                )
              },
              trailingIcon = {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = stringResource(R.string.custom_tags_clear),
                  modifier = Modifier
                    .size(16.dp)
                    .clickable {
                      val remaining = localValues.filterNot { it.equals(tagItem, ignoreCase = true) }
                      localValues = remaining
                      onValueChange(remaining.joinToString("; "))
                    }
                )
              }
            )
          }
        }

        // Drop gap expanding after the last chip
        val isGapAfterLast = (hoverDropSlot == localValues.size && draggingIndex != null && draggingIndex != localValues.size - 1)
        val gapWidthAfter by animateDpAsState(
          targetValue = if (isGapAfterLast) 52.dp else 0.dp,
          animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
          label = "gap_after_last"
        )
        if (gapWidthAfter > 0.dp) {
          Box(
            modifier = Modifier
              .width(gapWidthAfter)
              .height(32.dp)
              .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
              )
              .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
              )
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
          quickSuggestions = quickSuggestions,
          allValues = allValues,
          isTagConfirmed = isTagConfirmed,
          onAdd = { newTag ->
            val updated = (localValues + newTag).distinct()
            localValues = updated
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
  quickSuggestions: List<String>,
  allValues: List<String>,
  isTagConfirmed: (String, String) -> Boolean,
  onAdd: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var text by remember { mutableStateOf("") }
  val filteredSuggestions = remember(text, quickSuggestions, allValues) {
    if (text.isBlank()) {
      quickSuggestions
    } else {
      allValues
        .filter { it.contains(text.trim(), ignoreCase = true) }
        .take(20)
    }
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
              val isConfirmed = isTagConfirmed(tagName, suggestion)
              SuggestionChip(
                onClick = { onAdd(suggestion) },
                label = { Text(suggestion) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                  containerColor = if (isConfirmed) {
                    MaterialTheme.colorScheme.primaryContainer
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant
                  },
                  labelColor = if (isConfirmed) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  }
                ),
                border = if (isConfirmed) {
                  null
                } else {
                  SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = MaterialTheme.colorScheme.outline
                  )
                }
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
